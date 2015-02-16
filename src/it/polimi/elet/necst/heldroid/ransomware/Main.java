package it.polimi.elet.necst.heldroid.ransomware;

import com.cybozu.labs.langdetect.DetectorFactory;
import com.cybozu.labs.langdetect.LangDetectException;
import it.polimi.elet.necst.heldroid.utils.Options;
import it.polimi.elet.necst.heldroid.apk.DecodingException;
import it.polimi.elet.necst.heldroid.ransomware.emulation.TrafficScanner;
import it.polimi.elet.necst.heldroid.ransomware.text.classification.*;
import it.polimi.elet.necst.heldroid.ransomware.text.scanning.*;
import opennlp.tools.sentdetect.*;
import opennlp.tools.stemmer.Stemmer;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import polyglot.ast.Throw;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.charset.Charset;

public class Main {
    public static void main(String args[]) throws IOException, ParserConfigurationException, DecodingException, LangDetectException, InterruptedException {
        if (args.length < 1) {
            printUsage();
            return;
        }

        String op = args[0];
        Options options = new Options(args);

        DetectorFactory.loadProfile(Globals.LANGUAGE_PROFILES_DIRECTORY);

        if (op.equals("scan")) {
            if (options.contains("-sequential"))
                MainScannerSequential.main(args);
            else
                MainScanner.main(args);
        } else if (op.equals("server"))
            MainServer.main(args);
        else if (op.equals("pcap"))
            pcapAnalysis(args);
        else if (op.equals("learn"))
            learnSentenceDetector(args);
        else
            printUsage();
    }

    private static void printUsage() {
        String jarName =
            new File(Main.class.getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .getPath())
                .getName();

        System.out.println(
            "java -jar " + jarName + " op args\n" +
            "With op = (scan|pcap|learn):\n" +
            "    scan directory output.csv: scan all apks in the specified directory and its subdirectories\n" +
            "    pcap directory: analyzes all pcaps in the second-level subdirectories of the specified directory\n" +
            "    learn lang text: learns a sentence detector model for language lang analyzing sentences\n" +
            "        from the given text file, one per line"
        );
    }


    private static void pcapAnalysis(String[] args) throws IOException {
        File dir = new File(args[1]);

        TextClassifierCollection textClassifierCollection = Factory.createClassifierCollection();
        HtmlScanner htmlScanner = new HtmlScanner(textClassifierCollection);
        TrafficScanner trafficScanner = new TrafficScanner(htmlScanner);

        htmlScanner.setAcceptanceStrategy(Factory.createAcceptanceStrategy());

        for (File resultDir : dir.listFiles()) {
            if (!resultDir.isDirectory() || !resultDir.getName().endsWith(".apk"))
                continue;

            File innerResultDir = resultDir.listFiles()[0];

            try {
                trafficScanner.setPcap(new File(innerResultDir, "network-dump.pcap"));

                AcceptanceStrategy.Result result = trafficScanner.analyze();

                System.out.println(String.format("%s - Detected: %b ; Score: %f", resultDir.getName(), result.isAccepted(), result.getScore()));
            } catch (Exception e) { }
        }
    }


    private static void learnSentenceDetector(String[] args) throws IOException {
        File trainingFile = new File(args[2]);
        String language = args[1];

        Charset charset = Charset.forName("UTF-8");
        ObjectStream<String> lineStream = new PlainTextByLineStream(new FileInputStream(trainingFile), charset);
        ObjectStream<SentenceSample> sampleStream = new SentenceSampleStream(lineStream);

        SentenceModel model;

        try {
            model = SentenceDetectorME.train(language, sampleStream, true, null, TrainingParameters.defaultParams());
        } finally {
            sampleStream.close();
        }

        File modelFile = new File(Globals.MODELS_DIRECTORY, language + "-sent.bin");
        OutputStream modelStream = null;

        try {
            modelStream = new BufferedOutputStream(new FileOutputStream(modelFile));
            model.serialize(modelStream);
        } finally {
            if (modelStream != null)
                modelStream.close();
        }
    }

    private static String sanitize(String str) {
        return str.trim().replace("\"", "");
    }
}
