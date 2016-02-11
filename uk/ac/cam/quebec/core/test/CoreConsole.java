/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.cam.quebec.core.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import uk.ac.cam.quebec.core.ControlInterface;
import uk.ac.cam.quebec.core.GroupProjectCore;
import uk.ac.cam.quebec.dbwrapper.Database;
import uk.ac.cam.quebec.trends.Trend;
import uk.ac.cam.quebec.trends.TrendsQueue;
import uk.ac.cam.quebec.wikiwrapper.WikiException;

/**
 *
 * @author James
 */
public class CoreConsole extends Thread {

    private final BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
    private final GroupProjectCore core;
    private final ControlInterface coreInter;
    private final TrendsQueue coreTrends;
    private final String[] twitterCreds;
    private boolean running = true;

    public void processCommand(String command) throws WikiException {
        if (command.equalsIgnoreCase("start")) {
            if (!coreInter.isRunning()) {
                System.out.println("Starting Core");
                core.start();
            }
        } else if (command.equalsIgnoreCase("exit")) {
            if (coreInter.isRunning()) {
                System.out.println("Closing Core");
                coreInter.beginClose();
            }
            running = false;
        } else if (command.equalsIgnoreCase("status")) {
            System.out.println(coreInter.getServerInfo());
        } else if (command.equalsIgnoreCase("test twitter")) {
            System.out.println("Twitter test start");
            uk.ac.cam.quebec.twitterwrapper.test.Test.main(twitterCreds);
            System.out.println("Twitter test end");
        }else if (command.equalsIgnoreCase("test wikiproc")) {
            System.out.println("Wiki processing test start");
            uk.ac.cam.quebec.wikiproc.WikiProcessorTest.main(new String[0]);
            System.out.println("Wiki processing test end");
        }else if (command.equalsIgnoreCase("test wikiwrap")) {
            System.out.println("Wiki wrapper test start");
            uk.ac.cam.quebec.wikiwrapper.test.Test.main(new String[0]);
            System.out.println("Wiki wrapper test end");
        }else if (command.startsWith("add trend "))
        {   String s = command.substring(10);
            System.out.println("Adding trend "+s);
            Trend T = new Trend(s,"World",0);
            if(coreTrends.putTrend(T))
            {
                System.out.println("Trend "+s+" added successfully");
            }
            else
            {
                System.out.println("Failed to add trend "+s);
            }
            
            
        }
        else if (command.equalsIgnoreCase("Repopulate trends"))
        {
            System.out.println("Repopulating trends");
            coreInter.repopulateTrends();
        }
        else
        {
            System.out.println("Invalid command");
        }
    }

    public CoreConsole(GroupProjectCore _core, String[] _args) {
        core = _core;
        coreInter = _core;
        coreTrends = _core;
        twitterCreds = _args;
    }

    @Override
    public void run() {
        try {
            String s;
            System.out.println("Console initialised:");
            while ((running) && ((s = r.readLine()).length() > 0)) {
                try
                {
                processCommand(s);
                }
                catch (Exception ex)
                {
                    System.err.println(ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CoreConsole.class.getName()).log(Level.SEVERE, null, ex);
        }
    }   
    public static Document getConfig (String path)
    {   try {
        File inputFile = new File(path);
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(inputFile);
        doc.getDocumentElement().normalize();
        return doc;
        } catch (ParserConfigurationException| SAXException | IOException ex) {
            Logger.getLogger(CoreConsole.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }
    public static String[] getTwitterArgs(Document doc)
    {
        String[] twittercreds = new String[5];
            if(doc != null)
            {   NodeList parents = doc.getElementsByTagName("Twitter");
                Element parent = (Element)parents.item(0);
                NodeList Item = parent.getElementsByTagName("OAuth_Key");
                twittercreds[0] = Item.item(0).getTextContent();
                Item = parent.getElementsByTagName("OAuth_Secret");
                twittercreds[1] = Item.item(0).getTextContent();
                Item = parent.getElementsByTagName("Access_Token");
                twittercreds[2] = Item.item(0).getTextContent();
                Item = parent.getElementsByTagName("Access_Secret");
                twittercreds[3] = Item.item(0).getTextContent();
                Item = parent.getElementsByTagName("Account_Name");
                twittercreds[4] = Item.item(0).getTextContent();
            }
            return twittercreds;
    }   public static Database getDatabase(Document doc)
    {   
        NodeList parents = doc.getElementsByTagName("Database");
        Element parent = (Element)parents.item(0);
        NodeList Item = parent.getElementsByTagName("UserName");
        String User = Item.item(0).getTextContent();
        Item = parent.getElementsByTagName("Password");
        String Password = Item.item(0).getTextContent();
        Item = parent.getElementsByTagName("Path");
        String Path = Item.item(0).getTextContent();
        Item = parent.getElementsByTagName("ClearOnStart");
        String clear = Item.item(0).getTextContent();
        boolean wipe = clear.equalsIgnoreCase("true");
        Database.setCredentials(User, Password, "jdbc:mysql://"+Path,wipe);
            Database DB = Database.getInstance();
            return DB;
    }
        public static void main(String[] args) {
        try {
            Document doc = getConfig(args[0]);
            String[] test = getTwitterArgs(doc);
            
            //Database.setCredentials("IBUser", "IBUserTest", "jdbc:mysql://localhost:3306/ibprojectdb",false);
            Database DB = getDatabase(doc);//Database.getInstance();
            GroupProjectCore core = new GroupProjectCore(test, DB);
            core.setDaemon(true);
            core.setName("CoreThread");
            CoreConsole c = new CoreConsole(core, args);
            c.run();
        } catch (Exception ex) {
            System.err.println(ex);
        }
    }
}
