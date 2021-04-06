package com.ats_connection.asa.orchestrator.listener;

import java.util.concurrent.TimeUnit;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.http.HttpServlet;

import org.apache.log4j.Logger;


// Imports de MenuManager
import com.ats.menu_manager.ApiManager;

// Imports de StateMachine
import com.ats_connection.asa.orchestrator.sme.api.SMEFacade;

/**
 *
 * @author pdiaz
 *
 */
public class OrchListener extends HttpServlet implements ServletContextListener {

    private static final Logger logger = Logger.getLogger(OrchListener.class);

    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        logger.info("OrchListener: Destruyendo contexto ...");

        try {
            SMEFacade.getInstance().shutdown(30, TimeUnit.SECONDS);
            logger.info("OrchListener: 'SME' destruido exitosamente");
        } catch (Throwable e) {
            logger.error("OrchListener: Error destruyendo 'SME' ", e);
        }

        try {
            com.ats_connection.asa.server.logger.AsaServerLogger.getInstance().shutdown();
            logger.info("OrchListener: 'Logger' destruido exitosamente");
        } catch (Throwable e) {
            logger.error("OrchListener: Error destruyendo 'Logger' ", e);
        }

        try {
            com.ats_connection.asa.orchestrator.mgm.AsaOrchestratorManagement.getInstance().shutdown();
            logger.info("OrchListener: 'Mgm (stats)' destruido exitosamente");
        } catch (Throwable e) {
            logger.error("OrchListener: Error destruyendo 'Mgm (stats)' ", e);
        }
        
        try {
			ApiManager.getInstance().shutdown(1);
			logger.info("OrchListener: 'MenuManager' destruido exitosamente");
		} catch (InterruptedException e) {
			logger.error("OrchListener: Error destruyendo 'MenuManager' ", e);
		}
        
        logger.info("OrchListener: Contexto destruido");
    }

    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        logger.info("OrchListener: Inicializando Contexto");

        logger.info("  OrchListener: Inicializando AsaServerLogger...");
        try {
            com.ats_connection.asa.server.logger.AsaServerLogger.getInstance();
        } catch (Exception ex) {
            logger.fatal("  OrchListener: AsaServerLogger Failed..." + ex);
        }

        logger.info("  OrchListener: Inicializando Statistics...");
        try {
            com.ats_connection.asa.orchestrator.mgm.AsaOrchestratorManagement.getInstance();
        } catch (Exception ex) {
            logger.fatal("  OrchListener: Statistics Failed..." + ex);
        }

        logger.info("  OrchListener: Inicializando MenuManager...");
        ApiManager.getInstance();
        
        logger.info("  OrchListener: Inicializando SME...");
        SMEFacade.getInstance();

        logger.info("OrchListener: Contexto Inicializado");
    }
}
