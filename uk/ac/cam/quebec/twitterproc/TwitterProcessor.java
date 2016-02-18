package uk.ac.cam.quebec.twitterproc;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import javafx.util.Pair;
import uk.ac.cam.quebec.common.VisibleForTesting;
import uk.ac.cam.quebec.dbwrapper.Database;
import uk.ac.cam.quebec.dbwrapper.DatabaseException;
import uk.ac.cam.quebec.trends.Trend;
import uk.ac.cam.quebec.twitterwrapper.TwitException;
import uk.ac.cam.quebec.twitterwrapper.TwitterLink;
import uk.ac.cam.quebec.util.WordCounter;
import uk.ac.cam.quebec.util.parsing.StopWords;
import uk.ac.cam.quebec.util.parsing.UtilParsing;
import uk.ac.cam.quebec.wikiproc.WikiProcessor;
import winterwell.jtwitter.Status;

/**
 * <p>
 * Class responsible for processing trends. It should extract relevant tweets,
 * and send them to the database. After that analyse them and use the data to
 * create a list of concepts that are passed to the WikiProcessor.
 *
 * <p>
 * Don't care about concurrency issues.
 *
 * @author Momchil
 */
public class TwitterProcessor {

    private static final int PERCENTAGE = 30;

    /**
     * Process a trend and in the end pass a list of concepts to the Wikipedia
     * Processor.
     *
     * @param trend The trend that should be processed.
     */
    public static void process(Trend trend) {
        if (doProcess(trend)) {
            WikiProcessor wp = new WikiProcessor();
            wp.process(trend);
        }
    }

    /**
     * Do the actual processing of the trend
     * @param trend The trend that should be processed.
     * @return true if trend sucessfully processed
     */
    public static boolean doProcess(Trend trend) {
        trend.setParsedName(UtilParsing.parseTrendName(trend.getName()));
        TwitterLink twitter;
        try {
            twitter = new TwitterLink();
            List<Status> tweets = twitter.getTweets(trend.getName());

            Database db = Database.getInstance();
            try {
                db.putTweets(tweets, trend);
                tweets = db.getTweets(trend); // It is possible to have old tweets in the database.
            } catch (DatabaseException e) {
                e.printStackTrace();
            }

            trend.setPopularity(calculatePopularity(tweets));
            extractConcepts(trend, tweets);
            return true;
        } catch (TwitException e) {
            // TODO Auto-generated catch block
            System.err.println("Could not create a TwitterLink object");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * <p>
     * Ad-hoc evaluation of the popularity of a trend based on the tweets
     * related to it. For each such tweet we consider its retweet count and the
     * "popularity" of its author (measured in number of followers).
     *
     * @param tweets The tweets for the trend.
     * @return Integer evaluation of the popularity of the trend based on these
     * tweets.
     */
    private static int calculatePopularity(List<Status> tweets) {
        int popularity = 0;
        if (tweets != null) {
            for (Status tweet : tweets) {
                if (tweet.retweetCount != -1) {
                    // retweetCount is not unknown.
                    popularity += 10 * tweet.retweetCount;
                }
                if (tweet.getUser() != null) {
                    // Add the "popularity" (number of followers) of the user who made the tweet.
                    popularity += tweet.getUser().getFollowersCount();
                }
            }
        }
        return popularity;
    }

    /**
     * <p>
     * Tries to extract the concepts from the tweets and add them to the trend.
     *
     * <p>
     * Currently relies on simple word count.
     *
     * @param trend The trend we are processing.
     * @param tweets The tweets related to this trend.
     */
    @VisibleForTesting
    static void extractConcepts(Trend trend, List<Status> tweetsBatch) {
        trend.addConcept(new Pair<String, Integer>(trend.getParsedName(), Integer.MAX_VALUE));

        if (tweetsBatch == null) {
            return;
        }

        List<String> tweets = filter(tweetsBatch);
        WordCounter wordCounter = new WordCounter();
        WordCounter hashTagCounter = new WordCounter();
        for (String tweet : tweets) {
            String text = UtilParsing.removeLinks(tweet);
            String[] words = text.replaceAll("[^a-zA-Z0-9@#-]", " ")
                    .trim()
                    .split("\\s+");
            for (String word : words) {
                if (word.startsWith("@")) {
                    // Discard usernames and links.
                    continue;
                } else if (word.startsWith("#")) {
                    // Certain hash tag.
                    if (!trend.getParsedName().toLowerCase().equals(
                            UtilParsing.parseTrendName(word).toLowerCase())) {
			// If the hash tag is different from the current trend, add it as a
                        // relevant trend.
                        hashTagCounter.addWord(word);
                    }
                } else {
                    if (word.length() > 2 && !StopWords.isStopWord(word)) {
                        // Discard short words (a.g. is, the, a, and, ...).
                        wordCounter.addWord(word);
                    }
                }
            }
        }

	Pair<String, Integer>[] orderedWords = wordCounter.getOrderedWordsAndCount();
	if (orderedWords != null) {
	    // Add top 5 words + all those which pass the threshold.
	    for (int i = 0; i < orderedWords.length; i++) {
		if (i < 5
		|| 100 * orderedWords[i].getValue() >= PERCENTAGE * orderedWords.length) {
		    trend.addConcept(orderedWords[i]);
		}
	    }
	}

	Pair<String, Integer>[] orderedHashTags = hashTagCounter.getOrderedWordsAndCount();
	if (orderedHashTags != null) {
	    // Add top 5 hash tags + all those which pass the threshold.
	    for (int i = 0; i < orderedHashTags.length; i++) {
		if (i < 5
		|| 100 * orderedHashTags[i].getValue() >= PERCENTAGE * orderedHashTags.length) {
		    trend.addRelatedHashTag(orderedHashTags[i]);
		}
	    }
	}
    }

    /**
     * The Twitter API and the Database can give us two or more same tweets. We
     * want to remove the duplicates.
     *
     * @param tweets
     * @return Filtered List of tweets (represented just as strings that must be
     * unique).
     */
    private static List<String> filter(List<Status> tweets) {
        HashSet<String> cache = new HashSet<String>();
        for (Status tweet : tweets) {
            cache.add(tweet.getDisplayText());
        }
        return new ArrayList<String>(cache);
    }

}
