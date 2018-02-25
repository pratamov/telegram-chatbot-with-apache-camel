package com.pratamov.mandarintyping;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
public class TelegramRouter extends RouteBuilder{
	
	Logger LOG = Logger.getLogger(TelegramRouter.class);
	private static Map<String, String> previousMessage = new HashMap<>();
	Map<String, String> database = new HashMap<>();
	
	@Override
	public void configure() throws Exception {
		
		File file = new ClassPathResource("database.txt").getFile();
		Scanner read = new Scanner(new FileInputStream(file));
		
		while (read.hasNextLine()) {
			String line = read.nextLine();
			String[] parts = line.split("=");
			database.put(parts[0], parts[1]);
		}

		read.close();
		
		from("telegram:bots")
			.process(new Processor() {

				@Override
				public void process(Exchange exchange) throws Exception {
					
					String message = exchange.getIn().getBody(String.class);
					String camelTelegramChatId = exchange.getIn().getHeader("CamelTelegramChatId", String.class);
					boolean init = !previousMessage.containsKey(camelTelegramChatId);
					
					exchange.getOut().setHeader("CamelTelegramChatId", camelTelegramChatId);
					exchange.getOut().setHeader("init", init);
					exchange.getOut().setHeader("correct", false);
					
					if (!init) {
						String msg = previousMessage.get(camelTelegramChatId);
						if (msg.equals(message)) {
							exchange.getOut().setBody(database.get(msg));
							exchange.getOut().setHeader("correct", true);
						}
						else {
							exchange.getOut().setBody(msg);
						}
					}
					
				}
				
			})
			.to("log:logger?showHeaders=true")
			.to("telegram:bots")
			.process(new Processor() {

				@Override
				public void process(Exchange exchange) throws Exception {
					
					boolean correct = exchange.getIn().getHeader("correct", Boolean.class);
					boolean init = exchange.getIn().getHeader("init", Boolean.class);
					String camelTelegramChatId = exchange.getIn().getHeader("CamelTelegramChatId", String.class);
					exchange.getOut().setHeader("CamelTelegramChatId", camelTelegramChatId);
					
					if (correct || init) {
						String newMsg = select();
						exchange.getOut().setBody(newMsg);
						previousMessage.put(camelTelegramChatId, newMsg);
					}
					
				}
				
			})
			.to("telegram:bots");
		
	}
	
	private String select() {
		Random random = new Random();
		List<String> keys = new ArrayList<String>(database.keySet());
		return keys.get(random.nextInt(keys.size()));
	}

}
