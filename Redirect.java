/*
 * $Id: EchoServlet.java,v 1.5 2003/06/22 12:32:15 fukuda Exp $
 */
package org.mobicents.servlet.sip.example;

import java.util.*;
import java.io.IOException;

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

	public Redirect() {
		super();
	}

	public void init() {
		factory = (SipFactory) getServletContext().getAttribute(SIP_FACTORY);
		RegistrarDB = new HashMap<String,String>();
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
		if (!RegistrarDB.containsKey(aor)  ) { // To AoR not in the database, reply 404
			if(getAttr(request.getHeader("To"),"sip:").contains("alerta@acme.pt")){
				if( !getAttr(request.getHeader("From"),"sip:").contains("gestor@acme.pt") ){
					request.getProxy().proxyTo(factory.createURI(RegistrarDB.get("sip:gestor@acme.pt")));
					
				}
			}
			else{
				SipServletResponse response; 
				response = request.createResponse(404);
				response.send();}	
			/*	request.createResponse(100).send();
	    	Proxy proxy = request.getProxy();
	    	ArrayList<URI> addrList = new ArrayList<URI>();
	    	addrList.add(factory.createURI("sip:announcement@127.0.0.1:5080"));
	    	proxy.proxyTo(addrList);
	    	SipServletRequest infoReq = factory.createRequest(request.getApplicationSession(), "INFO", "sip:server@a6.pt", "sip:announcement@127.0.0.1:5080");
	    	infoReq.send();*/

			/*	B2buaHelper b2bua= request.getB2buaHelper();
	    	SipServletRequest req = b2bua.createRequest(request);
	    	req.setRequestURI(factory.createURI("sip:announcement@127.0.0.1:5080"));
	    	req.send();*/
		}else{
			request.getProxy().proxyTo(factory.createURI(RegistrarDB.get(getAttr(request.getHeader("To"),"sip:"))));
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
