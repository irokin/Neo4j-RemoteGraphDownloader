package uk.ac.ncl;

import org.neo4j.driver.*;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;
import org.neo4j.graphdb.Entity;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Downloader {
    public static void loadRemoteToLocal(AuthToken token, String localGraphHome, int commitFreq, boolean saveProperty) {
        System.out.println("############################################");
        System.out.println("Connecting to remote database: " + token.uri + "\n");
        Driver driver = GraphDatabase.driver(token.uri, AuthTokens.basic(token.userName, token.password));
        TransactionConfig config = TransactionConfig.builder().build();
        GraphDatabaseService graph = createEmptyGraph(localGraphHome);

        Counter counter = new Counter();
        Map<Long, Long> nodeMap = new HashMap<>();
        try(Session session = driver.session()) {
            Result result = session.run("match (x) return x", config);

            while(result.hasNext()) {
                try (org.neo4j.graphdb.Transaction tx = graph.beginTx()) {
                    while(result.hasNext()) {
                        counter.tick();
                        Node oNode = result.next().get("x").asNode();
                        org.neo4j.graphdb.Node tNode = graph.createNode();
                        nodeMap.put(oNode.id(), tNode.getId());
                        for (String label : oNode.labels()) {
                            tNode.addLabel(Label.label(label));
                        }
                        if(saveProperty) setProperties(oNode.asMap(), tNode);
                        if(counter.count % commitFreq == 0) {
                            System.out.println("Created " + counter.count + " Nodes.");
                            break;
                        }
                    }
                    tx.success();
                }
            }
        }

        counter.reset();
        try(Session session = driver.session()) {
            Result result = session.run("match ()-[r]->() return r");

            while(result.hasNext()) {
                try(org.neo4j.graphdb.Transaction tx = graph.beginTx()) {
                    while(result.hasNext()) {
                        counter.tick();
                        Relationship oRel = result.next().get("r").asRelationship();
                        org.neo4j.graphdb.Node sNode = graph.getNodeById(nodeMap.get(oRel.startNodeId()));
                        org.neo4j.graphdb.Node eNode = graph.getNodeById(nodeMap.get(oRel.endNodeId()));
                        org.neo4j.graphdb.Relationship tRel = sNode.createRelationshipTo(eNode, RelationshipType.withName(oRel.type()));
                        if(saveProperty) setProperties(oRel.asMap(), tRel);
                        if(counter.count % commitFreq == 0) {
                            System.out.println("Created " + counter.count + " Relationships.");
                            break;
                        }
                    }
                    tx.success();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }

        try(org.neo4j.graphdb.Transaction tx = graph.beginTx()) {
            System.out.println("\nDownloaded #Nodes = " + graph.getAllNodes().stream().count());
            System.out.println("Downloaded #Rels = " + graph.getAllRelationships().stream().count());
            System.out.println("############################################");
            tx.success();
        }

        driver.close();
    }

    public static GraphDatabaseService createEmptyGraph(String home) {
        File homeFile = new File(home);
        if(!homeFile.exists())
            homeFile.mkdir();
        System.out.println("Created new Neo4j graph at: " + new File(home, "databases/graph.db").getAbsolutePath() + "\n");
        deleteDirectory(new File(home, "databases"));
        return loadGraph(home);
    }

    public static GraphDatabaseService loadGraph(String home) {
        GraphDatabaseService graph = new GraphDatabaseFactory()
                .newEmbeddedDatabase(new File(home, "databases/graph.db"));
        Runtime.getRuntime().addShutdownHook(new Thread(graph::shutdown));
        return graph;
    }

    private static void deleteDirectory(File file) {
        File[] contents = file.listFiles();
        if(contents != null) {
            for (File content : contents) {
                deleteDirectory(content);
            }
        }
        file.delete();
    }

    private static void setProperties(Map<String, Object> propertyMap, Entity entity) {
        for (Map.Entry<String, Object> entry : propertyMap.entrySet()) {
            if(entry.getValue() instanceof Collection) {
                entity.setProperty(entry.getKey(), ((Collection) entry.getValue()).toArray(new String[0]));
            } else
                entity.setProperty(entry.getKey(), entry.getValue());
        }
    }

    static class Counter {
        int count = 0;
        public void tick() { count++; }
        public void reset() { count=0; }
    }
}
