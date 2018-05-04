


import cc.mallet.util.*;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.chart.util.Rotation;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.general.PieDataset;

public class TopicModel extends JFrame{
	//"/home/desty/uni/semester2/knowledge_discovery/Mallet/stoplists/en.txt"
	private static File STOPWORDS_FILE;
	private static final long serialVersionUID = 1L;
	
	private PiePlot3D plot;

	public TopicModel(String title) {
		super(title);
		Container content = getContentPane();
		
		DefaultPieDataset dataSet = new DefaultPieDataset();
		dataSet.setValue("Test", 100);
		
		JFreeChart chart = ChartFactory.createPieChart3D("ChartTitle", dataSet, true, true, false);
		plot = (PiePlot3D)chart.getPlot();
		plot.setStartAngle(290);
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setForegroundAlpha(0.5f);
		
		ChartPanel cpanel = new ChartPanel(chart);
		cpanel.setPreferredSize(new Dimension(500, 300));
		
		cpanel.addChartMouseListener(new ChartMouseListener() {
			@Override
			public void chartMouseMoved(ChartMouseEvent e) {
			}
			
			@Override
			public void chartMouseClicked(ChartMouseEvent e) {
				ChartEntity ce = e.getEntity();
				if (ce instanceof PieSectionEntity) {
					PieSectionEntity slice = (PieSectionEntity) ce;
					PiePlot plot = (PiePlot) chart.getPlot();
					
					int sliceIndex = slice.getSectionIndex();
					String sliceName = plot.getDataset().getKeys().get(sliceIndex).toString();
					System.out.println(sliceName);
				
				}
			}
		});
		
		content.setLayout(new FlowLayout());
		
		
		final TopicModel that = this;
		JButton btnFileSelect = new JButton("Select file for analysis");
		btnFileSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				FileDialog dia = new FileDialog(that, "Select a file");
				dia.setVisible(true);
				String filename = dia.getFile();
				if (filename != null) {
					File file = new File(dia.getDirectory() + "/" + filename);
					try {
						loadData(file);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		content.add(btnFileSelect);
		content.add(cpanel);
	}
	
	public static File loadOrGenerateStopwordPathFile(String stoplistpath, boolean overwrite) {
		try {
			File configFile = new File("config.cfg");
			if(!configFile.exists() || overwrite) {
					if (stoplistpath != null) {
						try (PrintWriter out = new PrintWriter(configFile)) {
						    out.println(stoplistpath);
						    out.close();
						}
					}
			}

			String configuredPath = FileUtils.readFile(configFile)[0];
			return new File(configuredPath);
		} catch(Exception e) {
			System.out.println("Something went wrong while loading/generating config");
			System.exit(2);
		}
		return null;
	}
	
	public void loadData(File file) throws IOException{
        // Begin by importing documents from text to feature sequences
        ArrayList<Pipe> pipeList = new ArrayList<Pipe>();

        // Pipes: lowercase, tokenize, remove stopwords, map to features
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(STOPWORDS_FILE, "UTF-8", false, false, false) );
        pipeList.add( new TokenSequence2FeatureSequence());

        InstanceList instances = new InstanceList (new SerialPipes(pipeList));

        Reader fileReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
        instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                                               3, 2, 1)); // data, label, name fields

        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        int numTopics = 20;
        ParallelTopicModel model = new ParallelTopicModel(numTopics, 1.0, 0.01);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(8);

        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(50);
        model.estimate();

        // Show the words and topics in the first instance

        // The data alphabet maps word IDs to strings
        Alphabet dataAlphabet = instances.getDataAlphabet();
        
        FeatureSequence tokens = (FeatureSequence) model.getData().get(0).instance.getData();
        LabelSequence topics = model.getData().get(0).topicSequence;
        
        Formatter out = new Formatter(new StringBuilder(), Locale.US);
        for (int position = 0; position < tokens.getLength(); position++) {
            out.format("%s-%d ", dataAlphabet.lookupObject(tokens.getIndexAtPosition(position)), topics.getIndexAtPosition(position));
        }
        System.out.println(out);
        
        // Estimate the topic distribution of the first instance, 
        //  given the current Gibbs state.
        double[] topicDistribution = model.getTopicProbabilities(0);

        // Get an array of sorted sets of word ID/count pairs
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        
        // Show top 5 words in topics with proportions for the first document
        for (int topic = 0; topic < numTopics; topic++) {
            Iterator<IDSorter> iterator = topicSortedWords.get(topic).iterator();
            
            out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
            int rank = 0;
            while (iterator.hasNext() && rank < 5) {
                IDSorter idCountPair = iterator.next();
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());
                rank++;
            }
            System.out.println(out);
        }
        
        // Create a new instance with high probability of topic 0
        StringBuilder topicZeroText = new StringBuilder();
        Iterator<IDSorter> iterator = topicSortedWords.get(0).iterator();

        int rank = 0;
        while (iterator.hasNext() && rank < 5) {
            IDSorter idCountPair = iterator.next();
            topicZeroText.append(dataAlphabet.lookupObject(idCountPair.getID()) + " ");
            rank++;
        }

        // Create a new instance named "test instance" with empty target and source fields.
        InstanceList testing = new InstanceList(instances.getPipe());
        testing.addThruPipe(new Instance(topicZeroText.toString(), null, "test instance", null));

        TopicInferencer inferencer = model.getInferencer();
        double[] testProbabilities = inferencer.getSampledDistribution(testing.get(0), 10, 1, 5);
        System.out.println("0\t" + testProbabilities[0]);
        
        
        // Show top 5 words in topics with proportions for the first document
        ((DefaultPieDataset) plot.getDataset()).clear();
        for (int topic = 0; topic < 1; topic++) {
            Iterator<IDSorter> iter = topicSortedWords.get(topic).iterator();
            
            out = new Formatter(new StringBuilder(), Locale.US);
            out.format("%d\t%.3f\t", topic, topicDistribution[topic]);
            int r = 0;
            while (iter.hasNext() && r < 5) {
                IDSorter idCountPair = iter.next();
                out.format("%s (%.0f) ", dataAlphabet.lookupObject(idCountPair.getID()), idCountPair.getWeight());

            	((DefaultPieDataset) plot.getDataset()).setValue(dataAlphabet.lookupObject(idCountPair.getID()).toString(), idCountPair.getWeight());
                r++;
            }
            System.out.println(out);
        }
	}
	
    public static void main(String[] args) throws Exception {
    	STOPWORDS_FILE = loadOrGenerateStopwordPathFile(args[0], false);
    	TopicModel main = new TopicModel("Topic Modelling Visualization");
		main.pack();
		main.setVisible(true);
    }

}

