package com.neo4j.loopy.commands;

import com.neo4j.loopy.LoopyApplication;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

import java.util.concurrent.Callable;

/**
 * Run command - Execute load test (default behavior)
 */
@Command(name = "run", 
         description = "Execute Neo4j load test",
         mixinStandardHelpOptions = true)
public class RunCommand implements Callable<Integer> {
    
    @ParentCommand
    private LoopyApplication parent;
    
    @Override
    public Integer call() throws Exception {
        // Delegate to parent application's call method
        return parent.call();
    }
}