
organization := "com.github.ingarabr"

name := "metric-insights"

scalaVersion := "2.10.4"

libraryDependencies ++= {
    object Versions {
        val unfiltered = "0.8.0"
        val rapture    = "0.9.1"
        val akka       = "2.3.3"
    }
    Seq(
        "net.databinder"          %% "unfiltered-filter"      % Versions.unfiltered,
        "net.databinder"          %% "unfiltered-jetty"       % Versions.unfiltered,
        "net.databinder.dispatch" %% "dispatch-core"          % "0.11.1",
        "com.propensive"          %% "rapture-io"             % Versions.rapture,
        "com.propensive"          %% "rapture-json"           % Versions.rapture,
        "com.propensive"          %% "rapture-json-jackson"   % "0.9.0",
        "com.propensive"          %% "rapture-json-jawn"      % "0.9.0",
        "com.typesafe.akka"       %% "akka-actor"             % Versions.akka,
        "com.typesafe.akka"       %% "akka-testkit"           % Versions.akka           % "test",
        "com.typesafe"             % "config"                 % "1.2.1",
        "org.elasticsearch"        % "elasticsearch"          % "1.0.1",
        "org.scalatest"            % "scalatest_2.10"         % "2.2.0"                 % "test",
        "org.mockito"              % "mockito-core"           % "1.9.5"                 % "test",
        "ch.qos.logback"           % "logback-classic"        % "1.1.2"
    )
}


