# MsgExchangeApp

A simple http message exchange web app using the RabbitMQ client.

## Prerequisites
tomcat9 and rabbitmq-server v3.8.2 must be installed in your system.

## How to test the app
Ensure that Apache Tomcat and RabbitMQ servers are up and running in localhost,  configured in their respective portals(8080 and 5672).

In your Apache Tomcat web manager, deploy the **war** file and run the app in your browser via


```bash
 localhost:8080/MsgExchangeApp/
```

Access the rabbitmq management interface via
```bash
localhost:15672/
```
