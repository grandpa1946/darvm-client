# darvm-websocket-relay

This system works as a translation layer, allowing the HTML5 and Javascript in the web browser to talk to the darvm-server instances running in your VMs. Websockets go in, TCP comes out. It also has a built in management system, where you can give each of your friends a username and password and define who's VM belongs to who. When you sign in to the client, the 'Server Address' is the address of your websocket relay, not your VM. The websocket relay will make that connection for you (see the config files).
