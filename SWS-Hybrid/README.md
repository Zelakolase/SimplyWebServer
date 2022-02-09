# SimplyWebServer - Hybrid
## Most of the information is found in *SWS/README.md*.
## Execute `$ javac Server.java && java Server --help` to navigate arguments.

## Examples for commands
- `# java Server --port 80 --proto http --max-concurrent-requests 1000 --backlog 5000 --gzip 1 --dynamic 1 --max-req-size 1000 --app-command "java app %request%"`. This will start HTTP Server at port 80 with *app.java* being the web app. However, it's not necessary to do that as could've easily used SWS.
- `# java Server --port 80 --proto http --max-concurrent-requests 1000 --backlog 5000 --gzip 1 --dynamic 1 --max-req-size 1000 --app-command "python stuff.py %request%"`. This will start HTTP Server at port 80 with *stuff.py* being the web app.
- `# ./server --port 80 --proto http --max-concurrent-requests 1000 --backlog 5000 --gzip 1 --dynamic 1 --max-req-size 1000 --app-command "python stuff.py %request%"`. This will start HTTP Server at port 80 with *stuff.py* being the web app. It's a lot faster and it doesn't require JVM.

## How to make your web app
You've to make a file where it's the entry point of processing a request. Your code will take the request at **stdin** in the form `45;55;115`. Each number is a decimal format for a letter [More](https://cryptii.com/pipes/text-decimal).
Then, you'll return a response with the following format : `45;55;115,HTTP/1.1 200 OK,text/html`. The format explains itself.