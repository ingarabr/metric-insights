import urllib2
import json
import time
import socket
import re
import sys
import platform
import traceback
import collections

hostname = platform.node()
fqdn = socket.getfqdn()
interval = 60

last_time = time.time()
oldCpu = None
oldNet = {}
prev_stats = None



def test_disks():
    suggested_devices = []
    with open("/proc/diskstats", "rt") as f:
        for line in f:
            skip = False
            _, _, name, reads, _, rbytes, rtime, writes, _, wbytes, wtime, io_in_progress, total_io_time = line.split()[:13]
            if int(reads) == 0 and int(writes) == 0:
                skip = True
            for od in suggested_devices:
                if od in name:
                    skip = True
            if not skip:
                suggested_devices.append(name)
    return suggested_devices

suggested_devices = test_disks()

def get_cpu():
    global oldCpu
    with open('/proc/stat') as f:
        for line in f:
            if re.match('^cpu ', line):
                cpu = [int(x) for x in line.split()[1:]]
                value = None

                if oldCpu:
                    diff_total = sum(cpu) - sum(oldCpu)
                    idleDiff = cpu[3] - oldCpu[3]
                    cpu_percent = float(diff_total - idleDiff) / diff_total

                    value = {
                        'percent': cpu_percent*100,
                        'user': float(cpu[0] - oldCpu[0])/diff_total,
                        'nice': float(cpu[1] - oldCpu[1])/diff_total,
                        'system': float(cpu[2] - oldCpu[2])/diff_total,
                        'idle': float(cpu[3] - oldCpu[3])/diff_total,
                        'iowait': float(cpu[4] - oldCpu[4])/diff_total,
                        'irq': float(cpu[5] - oldCpu[5])/diff_total,
                        'softirq': float(cpu[6] - oldCpu[6])/diff_total,
                        'steal': float(cpu[7] - oldCpu[7])/diff_total,
                        'guest': float(cpu[8] - oldCpu[8])/diff_total
                    }

                oldCpu = cpu

                return value


def net_io_counters():
    global oldNet
    f = open("/proc/net/dev", "rt")
    try:
        lines = f.readlines()
    finally:
        f.close()

    netvals = collections.defaultdict(dict)
    retdict = collections.defaultdict(dict)
    for line in lines[2:]:
        colon = line.rfind(':')
        assert colon > 0, repr(line)
        name = line[:colon].strip()
        fields = line[colon + 1:].strip().split()
        bytes_recv = int(fields[0])
        packets_recv = int(fields[1])
        errin = int(fields[2])
        dropin = int(fields[3])
        bytes_sent = int(fields[8])
        packets_sent = int(fields[9])
        errout = int(fields[10])
        dropout = int(fields[11])
        netvals[name] = {
            'bytes_sent':   bytes_sent,
            'bytes_recv': bytes_recv,
            'packets_sent': packets_sent,
            'packets_recv': packets_recv,
            'errin': errin,
            'errout': errout,
            'dropin': dropin,
            'dropout': dropout
        }
        if name in oldNet:
            for elem in ['bytes_sent', 'bytes_recv','packets_sent','packets_recv','errin','errout','dropin','dropout']:
                retdict[name][elem] = (netvals[name][elem] - oldNet[name][elem])/interval
        oldNet[name] = netvals[name]

    return retdict

def disk_io_counters():
    SECTOR_SIZE = 512
    retdict = {}
    f = open("/proc/diskstats", "rt")
    try:
        lines = f.readlines()
    finally:
        f.close()
    for line in lines:
        # http://www.mjmwired.net/kernel/Documentation/iostats.txt
        _, _, name, reads, _, rbytes, rtime, writes, _, wbytes, wtime, io_in_progress, total_io_time = line.split()[:13]
        if name in suggested_devices:
            rbytes = int(rbytes) * SECTOR_SIZE
            wbytes = int(wbytes) * SECTOR_SIZE
            reads = int(reads)
            writes = int(writes)
            rtime = int(rtime)
            wtime = int(wtime)
            io_time = int(total_io_time)
            retdict[name] = {'reads': reads, 'writes': writes, 'read_bytes': rbytes, 'write_bytes': wbytes, 'read_time': rtime, 'write_time': wtime, 'io_time': io_time}
    return retdict

def run_disk():
    global prev_stats
    stats = disk_io_counters()
    diffstats = {}
    if prev_stats:
        for (key, stat) in stats.iteritems():
            read_io = stat['reads'] - prev_stats[key]['reads']
            write_io = stat['writes'] - prev_stats[key]['writes']

            read_time = stat['read_time'] - prev_stats[key]['read_time']
            write_time = stat['write_time'] - prev_stats[key]['write_time']

            read_bytes = stat['read_bytes'] - prev_stats[key]['read_bytes']
            write_bytes = stat['write_bytes'] - prev_stats[key]['write_bytes']

            total_io_time = stat['io_time'] - prev_stats[key]['io_time']

            read_io_per_sec = read_io / interval
            write_io_per_sec = write_io / interval

            read_bytes_per_sec = read_bytes / interval
            write_bytes_per_sec = write_bytes / interval

            total_io = read_io + write_io
            total_io_per_sec = total_io / interval

            utilization = total_io_time / interval

            servicetime = utilization/total_io_per_sec if total_io_per_sec != 0 else 0

            average_wait = ( read_time + write_time ) / total_io if total_io != 0 else 0
            average_read_wait = read_time / read_io if read_io != 0 else 0
            average_write_wait = write_time / write_io if write_io != 0 else 0

            average_rq_size = ( read_bytes + write_bytes) / 1024 / total_io if total_io != 0 else 0
            average_rd_rq_size = read_bytes / read_io if read_io != 0 else 0
            average_write_rq_size = write_bytes / write_io if write_io != 0 else 0

            util_print = utilization / 10

            d = {}
            d['disk_name'] = key
            d['utilization'] = util_print
            d['servicetime'] = servicetime
            d['avg_wait'] = average_wait
            d['avg_read_wait'] = average_read_wait
            d['avg_write_wait'] = average_write_wait
            d['read_bytes_per_sec'] = read_bytes_per_sec
            d['write_bytes_per_sec'] = write_bytes_per_sec
            d['read_io_per_sec'] = read_io_per_sec
            d['write_io_per_sec'] = write_io_per_sec
            d['avg_rq_size'] = average_rq_size
            d['avg_read_rq_size'] = average_rd_rq_size
            d['avg_write_rq_size'] = average_write_rq_size
            diffstats[key] = d

    prev_stats = stats
    return diffstats

def run_mem():
    with open('/proc/meminfo') as f:
        m = {}
        for line in f:
            s = re.split(r':?\s+', line)
            m[s[0]] = s[1]
        free = int(m['MemFree']) + int(m['Buffers']) + int(m['Cached'])
        #used = int(m['MemFree']) + int(m['Buffers']) + int(m['Cached']) + int(m['Slab']) + int(m['PageTables']) + int(m['SwapCached'])
        free_percent = float(free)/int(m['MemTotal'])
        return {
            'used_percent': 1-free_percent,
            'used': (int(m['MemTotal']) - free)*1024,
            'unused': int(m['MemFree'])*1024,
            'cached': int(m['Cached'])*1024,
            'buffers': int(m['Buffers'])*1024,
            'swap': (int(m['SwapTotal']) - int(m['SwapFree']))*1024
        }

def add_doc(url, value):
    value['@timestamp'] = int(time.time()*1000)
    value['hostname'] = hostname
    value['fqdn'] = fqdn
    data = json.dumps(value, sort_keys=False)
    print data
    req = urllib2.Request(url, data)
    response = urllib2.urlopen(req)
    res = response.read()
    return res


while 1:
    try:
        start_time = time.time()
        cpu = get_cpu()
        if cpu:
            add_doc('http://localhost:64000/service/data/stats?type=cpu',  {'cpu': cpu } )
        net = net_io_counters()
        if net:
            add_doc('http://localhost:64000/service/data/stats?type=net', {'network': net})
        disk = run_disk()
        if disk:
            add_doc('http://localhost:64000/service/data/stats?type=disk', {'disk': disk})
        mem = run_mem()
        if mem:
            add_doc('http://localhost:64000/service/data/stats?type=mem', {'memory': mem})

        ttt = time.time()
        time_used = ttt - start_time
        last_time = ttt
        time.sleep(interval - time_used)
    except Exception as e:
        print e, traceback.format_exc()
        time.sleep(10)
