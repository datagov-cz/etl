package com.linkedpipes.etl.executor.monitor.execution;

import com.linkedpipes.etl.library.rdf.Statements;

import java.io.File;
import java.io.IOException;

class MessagesLoader {

    public Statements loadComponentMessages(
            Execution execution, String component) throws IOException {
        String name = component.substring(component.lastIndexOf("/") + 1);
        return this.loadMessagesFromFile(execution, name);
    }

    private Statements loadMessagesFromFile(Execution execution, String name)
            throws IOException {
        String fileName = name + ".trig";
        File file = new File(execution.getDirectory(), "messages/" + fileName);
        Statements statements = Statements.arrayList();
        statements.file().addAllIfExists(file);
        return statements;
    }

}
