/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.sip.SipServlet;
import javax.servlet.sip.SipServletMessage;
import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipServletResponse;
import javax.servlet.ServletException;
import javax.servlet.sip.URI;
import javax.servlet.sip.B2buaHelper;
import javax.servlet.sip.Proxy;
import javax.servlet.sip.SipFactory;

/**
 */
public class Redirect extends SipServlet {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	static private Map<String, String> RegistrarDB; // The Location Database 
	static private SipFactory factory;              // Factory for creating Req.s and URIs
	public ArrayList<String> List;
	public int number_calls = 0;
	public int number_messages = 0;
	public int number_conferences = 0;
	public Redirect() {
		super();
	}

	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		RegistrarDB = new HashMap<String,String>();
		List= new ArrayList<String>(); 
	}

	/**
	 * Acts as a registrar and location service for REGISTER messages
	 * @param  request The SIP message received by the AS 
	 */
	protected void doRegister(SipServletRequest request) throws ServletException,
	IOException {

		String toHeader = request.getHeader("To");
		String contactHeader = request.getHeader("Contact");
		String aor = getAttr(toHeader, "sip:");	
		String contact = getAttr(contactHeader, "sip:");
		String [] credenciais = aor.split("@");
		String dominio = credenciais[1];
		if(dominio.compareTo("acme.pt")==0){
			if(!contactHeader.contains("expires=0")){
				RegistrarDB.put(aor, contact);
				SipServletResponse response; 
				response = request.createResponse(200);
				response.send();
			}
			else{

				RegistrarDB.remove(aor);
				SipServletResponse response; 
				response = request.createResponse(200);
				response.send();
			}
		}
		else{

			SipServletResponse response; 
			response = request.createResponse(403);
			response.send();
		}
		// Some logs to show the content of the RegistrarDB database.
		log("REGISTER:******");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			log(pairs.getKey() + " = " + pairs.getValue());
		}
		log("REGISTER:******");
	}

	/**
	 * Sends SIP replies to INVITE messages
	 * - 300 if registred
	 * - 404 if not registred
	 * @param  request The SIP message received by the AS 
	 */
	protected void doInvite(SipServletRequest request)
			throws ServletException, IOException {
		// Some logs to show the content of the Registrar database.
		log("INVITE:***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			log(pairs.getKey() + " = " + pairs.getValue());
		}
		log("INVITE:***");
		String aor = getAttr(request.getHeader("To"), "sip:"); // Get the To AoR
		log("INVITE: To: " + aor);
		if (!RegistrarDB.containsKey(aor) && RegistrarDB.containsKey(getAttr(request.getHeader("From"), "sip:")) ) { // To AoR not in the database, reply 404
			if(getAttr(request.getHeader("To"),"sip:").contains("alerta@acme.pt")){
				if( !(getAttr(request.getHeader("From"),"sip:").contains("gestor@acme.pt") || getAttr(request.getHeader("From"),"sip:").contains("colaborador")) && RegistrarDB.containsKey("sip:gestor@acme.pt")){
					request.getProxy().proxyTo(factory.createURI(RegistrarDB.get("sip:gestor@acme.pt")));
					number_calls++;
					log("NUMBER OF CALLS: " + Integer.toString(number_calls));
					
				}else{
					
					SipServletResponse response; 
					response = request.createResponse(404);
					response.send();}
			}else{
				if(!(getAttr(request.getHeader("To"),"sip:").contains("sip:conferencia@acme.pt")) ){
					SipServletResponse response;
					response = request.createResponse(404);
					response.send();
				}
				else{
					if(getAttr(request.getHeader("From"),"sip:").contains("gestor") || getAttr(request.getHeader("From"),"sip:").contains("colaborador")){
						request.getProxy().proxyTo(factory.createURI("sip:conference@127.0.0.1:5070"));
						number_calls++;
						log("NUMBER OF CALLS: " + Integer.toString(number_calls));
					}else{
						SipServletResponse response; 
						response = request.createResponse(403);
						response.send();
						}
					}
				}
			}
		else{
			SipServletResponse response; 
			response = request.createResponse(403);
			response.send();}
	}

	protected void doMessage(SipServletRequest request)
			throws ServletException, IOException {
		// Some logs to show the content of the Registrar database.
		log("MESSAGE:***");
		Iterator<Map.Entry<String,String>> it = RegistrarDB.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<String,String> pairs = (Map.Entry<String,String>)it.next();
			log(pairs.getKey() + " = " + pairs.getValue());
		}
		log("MESSAGE:***");

		String aor = getAttr(request.getHeader("To"), "sip:"); // Get the To AoR
		log("MESSAGE: To: " + aor);
		if (!RegistrarDB.containsKey(aor) && RegistrarDB.containsKey(getAttr(request.getHeader("From"), "sip:")) ) { // To AoR not in the database, reply 404
			if(getAttr(request.getHeader("To"),"sip:").contains("alerta@acme.pt")){
				if( !getAttr(request.getHeader("From"),"sip:").contains("gestor") && RegistrarDB.containsKey("sip:gestor@acme.pt") ){
					request.getProxy().proxyTo(factory.createURI(RegistrarDB.get("sip:gestor@acme.pt")));
					number_messages++;
					log("NUMBER OF MESSAGES: " + Integer.toString(number_messages));
				}
				else{
					String s = new String(request.getRawContent(),StandardCharsets.UTF_8);
					if(s.contains("sip:colaborador")){
						String [] message = s.split(" ");
						if(RegistrarDB.containsKey(message[1])){
							if(message[0].contains("ADD")){
								List.add(message[1]);
								SipServletResponse response; 
								response = request.createResponse(200);
								response.send();
								for(int i =0; i<List.size();i++)
									log(List.get(i));
								number_messages++;
								log("NUMBER OF MESSAGES: " + Integer.toString(number_messages));
							}
							else if(message[0].contains("REMOVE")){
								List.remove(message[1]);
								SipServletResponse response; 
								response = request.createResponse(200);
								response.send();
								for(int i =0; i<List.size();i++)
									log(List.get(i));
								number_messages++;
								log("NUMBER OF MESSAGES: " + Integer.toString(number_messages));
							}
						}
						else{
							SipServletResponse response; 
							response = request.createResponse(404);
							response.send();
						}
					}
					else{
						if(s.contains("CONF")){

							for(Map.Entry<String,String> entry: RegistrarDB.entrySet()){
								if(!entry.getKey().contains("sip:gestor") && entry.getKey().contains("sip:colaborador")){
									SipServletRequest reques= factory.createRequest(request.getApplicationSession(),"MESSAGE","sip:alerta@acme.pt",RegistrarDB.get(entry.getKey()));
									reques.setContent("Call sip:conferencia@acme.pt".getBytes(), "text/plain");
									reques.send();
									SipServletResponse response; 
									response = request.createResponse(200);
									response.send();
								}

							}
							number_messages++;
							number_conferences++;
							log("NUMBER OF MESSAGES: " + Integer.toString(number_messages));
							log("NUMBER OF CONFERENCES: " + Integer.toString(number_conferences));
						}
						else{
							for(Map.Entry<String,String> entry: RegistrarDB.entrySet()){
								if(!entry.getKey().contains("sip:gestor"))
									request.getProxy().proxyTo(factory.createURI(RegistrarDB.get(entry.getKey())));
									number_messages++;
							}
							log("NUMBER OF MESSAGES: " + Integer.toString(number_messages));

						}
					}

				}
			}

			else{
				SipServletResponse response; 
				response = request.createResponse(404);
				response.send();}	
		}
		else{
			SipServletResponse response; 
			response = request.createResponse(404);
			response.send();
		}
	}

		


	/**
	 * Auxiliary function for extracting attribute values
	 * @param str the complete string
	 * @param attr the attr name 
	 * @return attr name and value 
	 */
	protected String getAttr(String str, String attr) {
		int indexStart = str.indexOf(attr);
		int indexStop  = str.indexOf(">", indexStart);
		if (indexStop == -1) {
			indexStop  = str.indexOf(";", indexStart);
			if (indexStop == -1) {
				indexStop = str.length();
			}
		}
		return str.substring(indexStart, indexStop);
	}



}