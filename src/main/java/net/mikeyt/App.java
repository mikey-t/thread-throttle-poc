package net.mikeyt;

import net.mikeyt.logic.QueueController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App 
{
    private static Logger log = LoggerFactory.getLogger(App.class);

    public static void main( String[] args )
    {
        System.out.println( "Throttling demo starting..." );

        new QueueController().start();

        System.out.println("QueueController started");
    }
}
