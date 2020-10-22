package com.barry_messaging;

import com.rabbitmq.client.*;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.HashMap;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;

import javax.servlet.AsyncContext;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@WebServlet(urlPatterns = {"/send"}, asyncSupported=true)
public class GreetingServlet extends HttpServlet {

	private final static String QUEUE_NAME = "signups";
	private static final String EXCHANGE_NAME = "barry";
	private static final String rmqServerUrl="amqp://guest:guest@localhost:5672/%2F";


	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

		response.setContentType("application/json;charset=UTF-8");

		//read in the incoming json map
		StringBuilder sb = new StringBuilder();
		BufferedReader br = request.getReader();
		String str;
		while ((str = br.readLine()) != null) {
			sb.append(str);
		}

		String json = sb.toString();
		br.close();

		//extract the values from the json object
		JsonReader jsonReader = Json.createReader(new StringReader(json));
		JsonObject jsonObject = jsonReader.readObject();

		String jsonName = jsonObject.getString("fullName");
		String jsonPhone= jsonObject.getString("phone");

		if( jsonName.isEmpty() || jsonPhone.isEmpty()){ //send 400 status if any of the two params are empty
			response.sendError(HttpServletResponse.SC_BAD_REQUEST);
		}
		else{ // build the string from the json map
			Map map = new HashMap<String, String>();

			map.put("fullName", jsonName);
			map.put("phone", jsonPhone);

			JSONObject rabbitmqJson = new JSONObject();
			rabbitmqJson.accumulate("user", map);

			String message = rabbitmqJson.toString();


			try {


				publishMessage(message); //publish the message


				ConnectionFactory factory = new ConnectionFactory(); // new connection for the consumer channel
				factory.setUri(rmqServerUrl);

				//start the async context so we can consume the messages published in rabbitmq server
				final AsyncContext asyncContext = request.startAsync(request, response);
				asyncContext.setTimeout(10000);
				String[] messages = {null};

				try(Connection connection = factory.newConnection();
					Channel channel = connection.createChannel()) {

					channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
					channel.queueDeclare(QUEUE_NAME, false, false, false, null);

					channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "signup");

					//create a consumer for this queue bind
					Consumer consumer = new DefaultConsumer(channel) {
						@Override
						public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
							String message1 = new String(body, "UTF-8");

							JsonReader reader = Json.createReader(new StringReader(message1));
							JsonObject object = reader.readObject();
							reader.close();

							JsonObject userObj = object.getJsonObject("user");
							String name = userObj.getString("fullName");

							String[] fullName;
							Map readMap = new HashMap<String, String>();


							if(name.contains(" ")){//split name if it contains whitespace
								fullName = name.split(" ");
								readMap.put("firstName", fullName[0]);
								readMap.put("lastName", fullName[1]);
							}
							else{
								readMap.put("fullName", name);

							}

							String phone = userObj.getString("phone");

							phone = !phone.contains("+33") ? "+33" + phone.trim() : phone.trim();

							readMap.put("phone", phone);

							JSONObject jsonObj = new JSONObject();
							jsonObj.accumulate("user", readMap);

							String newMsg = jsonObj.toString();

							messages[0] = newMsg;


							System.out.println("C [x] Received '" + message1 + "'");
						}
					};

					channel.basicConsume(QUEUE_NAME, false, "signup", consumer); // start consuming messages
					longProcessing();

					//get the published messages and return them as an async response back to the client
					JsonReader jsonReader1 = Json.createReader(new StringReader(messages[0]));
					JsonObject jsonObject1 = jsonReader1.readObject();

					jsonReader1.close();

					PrintWriter out = asyncContext.getResponse().getWriter();
					out.write(jsonObject1.toString());
					out.flush();

					asyncContext.complete();

				} catch (Exception e) {
					e.printStackTrace();
				}


			} catch ( Exception e) {
				e.printStackTrace();
			}
		}

	}

	public void publishMessage(String message) {

		//try to set up connection, exchange and queue bind before publishing
		ConnectionFactory cf = new ConnectionFactory();

		try(Connection con = cf.newConnection();
			Channel channel = con.createChannel()) {
			cf.setUri(rmqServerUrl);

			channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
			channel.queueDeclare(QUEUE_NAME, false, false, false, null);
			channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "signup");


			channel.basicPublish(EXCHANGE_NAME, "signup", null, message.getBytes("UTF-8"));
		}
		catch (Exception ex){
			ex.printStackTrace();
		}
	}

	private void longProcessing() {
		// wait for given time before finishing
		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}


}
