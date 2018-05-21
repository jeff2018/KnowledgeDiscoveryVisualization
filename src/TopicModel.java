


import cc.mallet.util.*;
import javafx.scene.layout.Border;
import cc.mallet.types.*;
import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.topics.*;

import java.util.*;
import java.util.regex.*;
import java.io.*;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;

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

import com.sun.glass.events.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JRadioButtonMenuItem;

public class TopicModel extends JFrame {
	//"/home/desty/uni/semester2/knowledge_discovery/Mallet/stoplists/en.txt"
	private static File STOPWORDS_FILE;
	private static final long serialVersionUID = 1L;
	
	private PiePlot3D plot;
	private ParallelTopicModel model;
	private JList<String> lstTopics;
	private int selectedTopic = -1;
	InstanceList instances;
	private JList wordList;
	private JList docList;
	
	public TopicModel(String title) {
		super(title);
		final TopicModel that = this;
		Container content = getContentPane();
		
		DefaultPieDataset dataSet = new DefaultPieDataset();
		dataSet.setValue("Test", 100);
		
		JFreeChart chart = ChartFactory.createPieChart3D("ChartTitle", dataSet, true, true, false);
		plot = (PiePlot3D)chart.getPlot();
		plot.setStartAngle(290);
		plot.setDirection(Rotation.CLOCKWISE);
		plot.setForegroundAlpha(0.5f);
		
		ChartPanel cpanel = new ChartPanel(chart);
		cpanel.setAlignmentX(Component.RIGHT_ALIGNMENT);
		cpanel.setAlignmentY(Component.TOP_ALIGNMENT);
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

					int docId = getDocId(sliceName);
			        List<Integer> rel = getRelevantDocuments(docId);
			        System.out.println(rel);
			        ((DefaultListModel)docList.getModel()).removeAllElements();
			        int k = 0;
			        for (Integer i : rel) {
			        	if (i.intValue() == docId) {
			        		continue;
		        		}
			        	String fn = instances.get(i).getName().toString();

			        	int lastSlash = fn.lastIndexOf("/");
			        	fn = fn.substring(lastSlash + 1);
			        	((DefaultListModel)docList.getModel()).addElement(fn);
			        	k++;
			        	if (k >= 5) break;
			        }
				}
			}
		});
		
		ActionListener loadCorpusActionListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				final JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
				//FileDialog dia = new DirectoryDialog(that, "Select a directory");
				//dia.setVisible(true);
				int selectState = fc.showOpenDialog(that); //dia.getFile();
				if (selectState == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					try {
						loadData(file);
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			};
		};
		getContentPane().setLayout(new BorderLayout(0, 0));
		content.add(cpanel, BorderLayout.CENTER);		
		cpanel.setLayout(new BoxLayout(cpanel, BoxLayout.X_AXIS));
		
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		getContentPane().add(scrollPane, BorderLayout.WEST);
		lstTopics = new JList<>();
		scrollPane.setViewportView(lstTopics);
		lstTopics.setAlignmentX(Component.LEFT_ALIGNMENT);
		lstTopics.setModel(new AbstractListModel<String>() {
			public int getSize() {
				if (that.model == null)
					return 0;
				return that.model.getNumTopics();
			}
			public String getElementAt(int index) {
				if (that.model == null)
					return "";
				System.out.println();
				ArrayList<TreeSet<IDSorter>> topicSortedWords = that.model.getSortedWords();
		        Alphabet dataAlphabet = model.getAlphabet();
		        
	            Iterator<IDSorter> iter = topicSortedWords.get(index).iterator();
	            String res  = "";
	            for (int i = 0; i < 3; i++) {
	            	if (iter.hasNext()) {
	            		if (!res.isEmpty()) {
	            			res += ", ";
	            		}
	            		IDSorter item = iter.next();
	            		res += dataAlphabet.lookupObject(item.getID()).toString();
	            	}
	            }
	            return res;
			}
		});
		lstTopics.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		
		lstTopics.addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				JList<String> list = (JList<String>)e.getSource();
				int selected = list.getSelectedIndex();
				String selectedTopicName = list.getModel().getElementAt(selected);
				chart.setTitle(selectedTopicName);
		        selectTopic(selected);
			}
		});
		
		JList lstWords = new JList();
		getContentPane().add(lstWords, BorderLayout.EAST);
		lstWords.setAlignmentX(Component.RIGHT_ALIGNMENT);
		
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.EAST);
		
		panel.setLayout(new GridLayout(2, 1));
		panel.setPreferredSize(new Dimension(300,300));
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.Y_AXIS));
		
		JLabel words = new JLabel("Most Frequent Words:");
		words.setAlignmentX(0);
		
		ArrayList<String> testWordList = new ArrayList<String>();
		wordList = new JList(new DefaultListModel<String>());
		//wordList.setPreferredSize(new Dimension(200,200));
		wordList.setAlignmentX(0);
		wordList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		JScrollPane listScroller = new JScrollPane(wordList);
		listScroller.setPreferredSize(new Dimension (250,80));
		listScroller.setAlignmentX(0);
		words.setLabelFor(wordList);
		p1.add(words);
		p1.add(listScroller);
		p1.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		
		
		JPanel p2 = new JPanel();
		p2.setBorder(BorderFactory.createLineBorder(Color.BLACK));
		p2.setLayout(new BoxLayout(p2, BoxLayout.Y_AXIS));
		
		JLabel documents = new JLabel("Similar Documents:");
		documents.setAlignmentX(0);
		docList = new JList(new DefaultListModel<String>());
		docList.setAlignmentX(0);
		docList.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
		JScrollPane listScroller2 = new JScrollPane(docList);
		listScroller2.setPreferredSize(new Dimension(250,80));
		listScroller2.setAlignmentX(0);
		documents.setLabelFor(docList);
		p2.add(documents);
		p2.add(listScroller2);
		p2.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		panel.add(p1);
		panel.add(p2);
		
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		
		JMenuItem mntmLoadCorpus = new JMenuItem("Load Corpus");
		mntmLoadCorpus.addActionListener(loadCorpusActionListener);
		menuBar.add(mntmLoadCorpus);
		
		JMenu menu = new JMenu("Visualization Type");
		menuBar.add(menu);
		
		ButtonGroup group = new ButtonGroup();
		JRadioButtonMenuItem rbMenuItem1 = new JRadioButtonMenuItem("Circle");
		rbMenuItem1.setSelected(true);
		rbMenuItem1.setMnemonic(KeyEvent.VK_R);
		group.add(rbMenuItem1);
		menu.add(rbMenuItem1);
		
		JRadioButtonMenuItem rbMenuItem2 = new JRadioButtonMenuItem("Option2");
		rbMenuItem2.setMnemonic(KeyEvent.VK_R);
		group.add(rbMenuItem2);
		menu.add(rbMenuItem2);
		
		
		
	}
	
	private int getDocId(String name) {
		for (int i = 0; i < instances.size(); i++) {
			Instance o = instances.get(i);
			if (o.getName().toString().endsWith("/" + name)) {
				return i;
			}
		}
		return -1;
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
        pipeList.add(new Input2CharSequence("UTF-8"));
        pipeList.add( new CharSequenceLowercase() );
        pipeList.add( new CharSequence2TokenSequence(Pattern.compile("\\p{L}[\\p{L}\\p{P}]+\\p{L}")) );
        pipeList.add( new TokenSequenceRemoveStopwords(STOPWORDS_FILE, "UTF-8", false, false, false) );
        pipeList.add(new TokenSequence2PorterStems());
        
        pipeList.add( new TokenSequence2FeatureSequence());
        pipeList.add(new Target2Label());
        //pipeList.add(new FeatureSequence2FeatureVector());
        pipeList.add(new PrintInputAndTarget());
        
        instances = new InstanceList (new SerialPipes(pipeList));

        if (file.isDirectory()) {
        	FileIterator iterator =
                    new FileIterator(file, new TxtFilter(),
                                     FileIterator.LAST_DIRECTORY);
	
	        instances.addThruPipe(iterator);
        } else {
            Reader fileReader = new InputStreamReader(new FileInputStream(file), "UTF-8");
            instances.addThruPipe(new CsvIterator (fileReader, Pattern.compile("^(\\S*)[\\s,]*(\\S*)[\\s,]*(.*)$"),
                                                   3, 2, 1)); // data, label, name fields
        }
        
        // Create a model with 100 topics, alpha_t = 0.01, beta_w = 0.01
        //  Note that the first parameter is passed as the sum over topics, while
        //  the second is the parameter for a single dimension of the Dirichlet prior.
        int numTopics = 20;
        model = new ParallelTopicModel(numTopics, 1.0, 0.01);

        model.addInstances(instances);

        // Use two parallel samplers, which each look at one half the corpus and combine
        //  statistics after every iteration.
        model.setNumThreads(8);

        // Run the model for 50 iterations and stop (this is for testing only, 
        //  for real applications, use 1000 to 2000 iterations)
        model.setNumIterations(50);
        model.estimate();
        
        lstTopics.updateUI();
        selectTopic(0);
	}
	
	public void selectTopic(int id) {
		this.selectedTopic = id;
		
		// update pichart
        ArrayList<TreeSet<IDSorter>> topicDocuments = model.getTopicDocuments(5); // 5 is smoothing parameter

        ((DefaultPieDataset) plot.getDataset()).clear();
        Iterator<IDSorter> dociter = topicDocuments.get(id).iterator();
        int r = 0;
        while (dociter.hasNext() && r < 5) {
        	int docid = dociter.next().getID();
            
        	String docName = instances.get(docid).getName().toString();
        	int lastSlash = docName.lastIndexOf("/");
        	docName = docName.substring(lastSlash + 1);
        	double probability = model.getTopicProbabilities(docid)[id];

        	((DefaultPieDataset) plot.getDataset()).setValue(docName, probability);
            r++;
        }
        if (dociter.hasNext()) {
        	double remainingProbability = 0;
            while (dociter.hasNext()) {
            	int docid = dociter.next().getID();
                double probability = model.getTopicProbabilities(docid)[id];
                remainingProbability += probability;
            }
            ((DefaultPieDataset) plot.getDataset()).setValue("other", remainingProbability);
        }
        
        // update wordslist
        ArrayList<TreeSet<IDSorter>> topicSortedWords = model.getSortedWords();
        Alphabet dataAlphabet = model.getAlphabet();
        
        ((DefaultListModel)(wordList.getModel())).removeAllElements();
        Iterator<IDSorter> iter = topicSortedWords.get(id).iterator();
        r = 0;
        while (iter.hasNext() && r < 10) {
            IDSorter idCountPair = iter.next();
            
            String word = dataAlphabet.lookupObject(idCountPair.getID()).toString();
            int wordCount = (int) Math.floor(idCountPair.getWeight());
            
            ((DefaultListModel<String>)(wordList.getModel())).addElement(word + " - " + wordCount);
            r++;
        }
        
        this.getContentPane().repaint();
	}
	
	private List<Integer> getRelevantDocuments(int id){
		TreeMap<Double, Integer> relevance = new TreeMap<>();
		int docCount = instances.size();

		double[] pid = model.getTopicProbabilities(id);
		for (int x = 0; x < docCount; x++) {
				double[] px = model.getTopicProbabilities(x);
				double dist = getEuclidDist(px, pid);
				relevance.put(dist, x);
				System.out.println(x + " - " + dist);
		}
		return new ArrayList<Integer>(relevance.values());
	}
	
	private double getEuclidDist(double[] a, double[] b) {
		if (a.length != b.length)
			throw new IllegalStateException("something very bad has happend");
		
		double res = 0;
		for (int i = 0; i < a.length; i++) {
			double c = a[i] - b[i];
			res += c*c;
		}
		
		return Math.sqrt(res);
	}
	
    public static void main(String[] args) throws Exception {
    	STOPWORDS_FILE = loadOrGenerateStopwordPathFile(args[0], false);
    	TopicModel main = new TopicModel("Topic Modelling Visualization");
		main.pack();
		main.setVisible(true);
    }
    
    class TxtFilter implements FileFilter {

        /** Test whether the string representation of the file 
         *   ends with the correct extension. Note that {@ref FileIterator}
         *   will only call this filter if the file is not a directory,
         *   so we do not need to test that it is a file.
         */
        public boolean accept(File file) {
            return file.toString().endsWith(".txt");
        }
    }

}

