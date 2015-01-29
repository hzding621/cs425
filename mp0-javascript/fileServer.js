const fs = require('fs');
const net = require('net');


var clientCount = 0;

var server = net.createServer(function(socket) {

	console.log("Client " + clientCount + " connected");
	clientCount++;

	var msg = "";
	var length = 0;
	var file = fs.createReadStream(process.argv[3]);
	file.on('data', function(chunk) {
		msg += chunk.toString();
		length += chunk.length;
	});
	file.on('end', function(){
		socket.write("LENGTH\n"+length+"\n");
		socket.write("CONTENT\n"+msg);
		socket.pipe(socket);
		socket.end();
	});

});
server.listen(process.argv[2]);