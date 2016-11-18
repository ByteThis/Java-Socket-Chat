# Java Socket based Chat

<h3>Compilation</h3>

`javac ChatClient.java`

`javac ChatServer.java`


<h3>Usage</h3>

`java ChatServer 8000` Launches the server listening on port `8000`

`java ChatClient localhost 8000` Launches the java GUI client and connects to `localhost` on port `8000`


<h3>Commands</h3>
`/nick name`	Choose `name`. It can not be already in use.

`/join room`	Enters `room` or creates it if it does not exist

`/leave`		Leaves the current chat room

`/bye`			Leaves the chat