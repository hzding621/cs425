const net = require('net');

var client = new net.Socket();
client.connect(process.argv[3], process.argv[2], function() {
	console.log("client: connecting to "+ process.argv[2]);
});

var msg = "";
var length = "";

client.on('data', function(data) {
	msg += data.toString();
})
client.on('close', function() {
	var array = msg.split("\n");
	if (array[0] === "LENGTH") {
		console.log("client: received " + Number(array[1]) + " bytes\n");
		if (array[2] === "CONTENT") {
			for (var i = 3; i<array.length; i++)
				console.log(array[i]);
		}
	}
})