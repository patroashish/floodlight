/**
 *    Copyright 2013, Big Switch Networks, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package net.floodlightcontroller.core;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import net.floodlightcontroller.core.coap.CoapConstants;
import net.floodlightcontroller.core.coap.CoapEngine;
import net.floodlightcontroller.core.coap.DatabaseCommitter;
import net.floodlightcontroller.core.coap.PolicyEngine;
import net.floodlightcontroller.core.coap.ThreadMonitor;
import net.floodlightcontroller.core.internal.CmdLineSettings;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.FloodlightModuleLoader;
import net.floodlightcontroller.core.module.IFloodlightModuleContext;
import net.floodlightcontroller.restserver.IRestApiService;

/**
 * Host for the Floodlight main method
 * @author alexreimers
 */
public class Main {

    /**
     * Main method to load configuration and modules
     * @param args
     * @throws FloodlightModuleException 
     */
    public static void main(String[] args) throws FloodlightModuleException {
        // Setup logger
        System.setProperty("org.restlet.engine.loggerFacadeClass", 
                "org.restlet.ext.slf4j.Slf4jLoggerFacade");
        
        CmdLineSettings settings = new CmdLineSettings();
        CmdLineParser parser = new CmdLineParser(settings);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            parser.printUsage(System.out);
            System.exit(1);
        }
        
        // Load modules
        FloodlightModuleLoader fml = new FloodlightModuleLoader();
        IFloodlightModuleContext moduleContext = fml.loadModulesFromConfig(settings.getModuleFile());
        // Run REST server
        IRestApiService restApi = moduleContext.getServiceImpl(IRestApiService.class);
        restApi.run();
        // Run the main floodlight module
        IFloodlightProviderService controller =
                moduleContext.getServiceImpl(IFloodlightProviderService.class);
        

        // Ashish: Begin mods for COAP
        // Add the COAP server code here.
        ThreadMonitor monitor = new ThreadMonitor();

        CoapEngine coapEngine = new CoapEngine(controller);
        Thread coapEngineThread = new Thread(coapEngine, "COAP main thread");
        coapEngineThread.start();
        monitor.add_thread_to_monitor(coapEngineThread, coapEngineThread.getName());

        // Run policy engine only when allowed to do so for the current instance.
        if (CoapConstants.USE_POLICY_ENGINE) {
          PolicyEngine policyEngine = new PolicyEngine(controller);
          Thread policyEngineThread = new Thread(policyEngine, "Policy engine");
          policyEngineThread.start();
          monitor.add_thread_to_monitor(policyEngineThread, policyEngineThread.getName());
        }
        
        // 0126 - Disabled the database committer due to crashes.
        /*
        DatabaseCommitter committer = new DatabaseCommitter();
        Thread committer_thread = new Thread(committer, "DB committer");
        committer_thread.start();
        monitor.add_thread_to_monitor(committer_thread, committer_thread.getName());
        */
        // Ashish: End mods for COAP
        
        // This call blocks, it has to be the last line in the main
        controller.run();
    }
}
