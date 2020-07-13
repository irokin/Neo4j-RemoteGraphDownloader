package uk.ac.ncl;

import org.apache.commons.cli.*;

public class Run {
    public static void main(String[] args) {
        Options options = new Options();

        options.addOption(Option.builder("uri").hasArg().desc("Database address in bolt protocol.").build());
        options.addOption(Option.builder("u").hasArg().desc("User name.").build());
        options.addOption(Option.builder("p").hasArg().desc("Password.").build());
        options.addOption(Option.builder("t").hasArg().desc("File directory to save the database.").build());
        options.addOption(Option.builder("i").hasArg().desc("Commit frequency. Default = 100000.").build());
        options.addOption(Option.builder("sp").desc("Save node/relationship properties.").build());

        options.addOption(Option.builder("h").longOpt("help").desc("Print help information.").build());
        String header = "Lightweight tool for downloading Neo4j database via bolt address.";
        HelpFormatter formatter = new HelpFormatter();

        try {
            CommandLine cmd = (new DefaultParser()).parse(options, args);
            if (cmd.hasOption("h")) {
                formatter.printHelp("-uri uri -u user -p password -t outFile", header, options, "", false);
                return;
            }

            int commitFreq = 100000;
            if (cmd.hasOption("i"))
                commitFreq = Integer.parseInt(cmd.getOptionValue("i"));

            boolean saveProps = false;
            if (cmd.hasOption("sp"))
                saveProps = true;

            AuthToken token = new AuthToken(cmd.getOptionValue("uri"), cmd.getOptionValue("u"), cmd.getOptionValue("p"));
            Downloader.loadRemoteToLocal(token, cmd.getOptionValue("t"), commitFreq, saveProps);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }



}
