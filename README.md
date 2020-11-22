# gRPC-based Chat System
## Table of contents
* [Authors](#Authors)
* [Assignment description](#Assignment-description)
* [Features](#Features)
* [More Info](#Info)

## Authors
* Romeo Permentier
* Nick Braeckman

## Assignment description
Implement a simple chat-system with following features:
* Server: is responsible for communication between users and keeps track of online users.
* Client: asks a username on startup. (Duplicate usernames are forbidden and therefore blocked)
* Private chat: chatting with someone who is online in the room must be possible.


## Features
### Private chat
* send private messages to 1 person at a time
* reading all the private messages, coming from all private users
* show received private messages after reopening the chat system
### Public chat
* click on a username to open the private chat and send messages to that user
### Login screen
* choose a username and chat server

## Clone the project

1. Clone project from github (master branch)

2. Import project 
   *  Project JDK: 1.8.0_241
   *  Project Language Level: 8
   
3. Build the gradle file
   1. Go to build.gradle
   2. Run task ```runServer(type: JavaExec)```
   3. Run task ```runClient(type: JavaExec```

4. Run configurations:
   * Port number: ```1000```
   * Hostname: ```localhost```
   
## More Info
* [gRPC](https://grpc.io/docs/languages/java/basics/)
* [protocol buffers](https://developers.google.com/protocol-buffers/docs/overview)
