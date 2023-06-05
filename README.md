# SimplyWebServer
A web server written in Java. The goal of SimplyWebServer is a simple, fast, and minimal web server. It is designed for Students, Small and Medium Sized Applications, and local/private systems!

## Build and Run
See src/test.

## Benchmarking
You need apache2-utils in order to run 'benchmark/login.sh'. You can download it using:
- If you're using a debian-based system: `# apt-get install apache2-utils`.
- If you're using a Redhat-based system: `# yum install httpd-tools`.
- If you're using a different system, check its manual.
In order to run the benchmark test, you need to run 'src/test/SimpleLogin' class at port 8080, then run the benchmark file 'benchmark/login.sh'. You run shell files in terminal by executing `$ chmod +x login.sh && ./login.sh`.

