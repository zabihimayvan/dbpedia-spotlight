package org.dbpedia.spotlight.candidate.cooccurrence.weka;

import org.dbpedia.spotlight.candidate.cooccurrence.features.CandidateFeatures;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.CandidateData;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.CoOccurrenceData;
import org.dbpedia.spotlight.candidate.cooccurrence.features.data.OccurrenceDataProvider;
import org.dbpedia.spotlight.exceptions.ItemNotFoundException;
import org.dbpedia.spotlight.model.SurfaceFormOccurrence;
import org.dbpedia.spotlight.model.TaggedText;
import org.dbpedia.spotlight.tagging.TaggedToken;
import weka.core.Attribute;
import weka.core.Instance;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @author Joachim Daiber
 */
public abstract class InstanceBuilderUnigram extends InstanceBuilder {

	public static Attribute unigram_count_corpus = new Attribute("count_corpus");
	public static Attribute unigram_count_web = new Attribute("count_web");

	public static Attribute bigram_left_significance_corpus = new Attribute("left_significance_corpus");
	public static Attribute bigram_left_count_corpus = new Attribute("left_count_corpus");
	public static Attribute bigram_left_significance_web = new Attribute("left_significance_web");
	public static Attribute bigram_left_count_web = new Attribute("left_count_web");

	public static Attribute bigram_right_significance_corpus = new Attribute("right_significance_corpus");
	public static Attribute bigram_right_count_corpus = new Attribute("right_count_corpus");
	public static Attribute bigram_right_significance_web = new Attribute("right_significance_web");
	public static Attribute bigram_right_count_web = new Attribute("right_count_web");

	public static Attribute trigram_left_count_web = new Attribute("left_trigram_count_web");
	public static Attribute trigram_right_count_web = new Attribute("right_trigram_count_web");
	public static Attribute trigram_middle_count_web = new Attribute("middle_trigram_count_web");

	public static Attribute next_to_uppercase = new Attribute("next_to_uppercase", Arrays.asList("not_next_to_uppercase", "next_to_uppercase"));
	public static Attribute non_sentence_initial_uppercase = new Attribute("non_sentence_initial_uppercase", Arrays.asList("lowercase", "starts_with_uppercase", "all_uppercase"));

	public static Attribute quoted = new Attribute("quoted", Arrays.asList("quoted", "not_quoted"));
	public static Attribute in_enumeration = new Attribute("in_enumeration", Arrays.asList("yes"));

	public static Attribute pre_pos = new Attribute("pre_POS", Arrays.asList("pp$", "prep", "of", "a", "the", "adj"));
	public static Attribute next_pos = new Attribute("next_POS", Arrays.asList("verb", "of", "for"));

	public static Attribute possesive = new Attribute("possesive", Arrays.asList("yes"));


	/**
	 * Default Thresholds:
	 */
	protected long unigramCorpusMax = 40000;
	protected long unigramWebMin = 0;
	protected long bigramLeftWebMin = 0;
	protected long bigramRightWebMin = 0;
	protected long trigramLeftWebMin = 0;
	protected long trigramRightWebMin = 0;
	protected long trigramMiddleWebMin = 0;


	protected InstanceBuilderUnigram(OccurrenceDataProvider dataProvider) {
		super(dataProvider);
	}


	public ArrayList<Attribute> buildAttributeList() {

		ArrayList<Attribute> attributeList = new ArrayList<Attribute>();

		unigram_count_corpus.setWeight(0.5);

		attributeList.add(unigram_count_corpus);

		unigram_count_web.setWeight(0.5);
		attributeList.add(unigram_count_web);

		//Left neighbour:
		attributeList.add(bigram_left_count_web);

		//Right neighbour:
		attributeList.add(bigram_right_count_web);

		trigram_left_count_web.setWeight(10);
		attributeList.add(trigram_left_count_web);

		trigram_right_count_web.setWeight(10);
		attributeList.add(trigram_right_count_web);

		trigram_middle_count_web.setWeight(10);
		attributeList.add(trigram_middle_count_web);

		//Other properties:
		attributeList.add(quoted);
		attributeList.add(possesive);

		in_enumeration.setWeight(50);
		attributeList.add(in_enumeration);


		//Case:
		attributeList.add(non_sentence_initial_uppercase);
		attributeList.add(next_to_uppercase);


		//Part-of-speech:
		attributeList.add(pre_pos);
		attributeList.add(next_pos);


		//Candidate class:
		attributeList.add(candidate_class);

		return attributeList;
	}

	@Override
	public Instance buildInstance(SurfaceFormOccurrence surfaceFormOccurrence, Instance instance) {

		List<Attribute> attributeList = buildAttributeList();


		/**
		 * Occurrence data of the candidate
		 */

		CandidateData candidateData = null;
		try {
			candidateData = dataProvider.getCandidateData(surfaceFormOccurrence.surfaceForm().name());
		} catch (ItemNotFoundException e) {

			/**
			 * No information about the candidate available.
			 *
			 * This means that no co-occurrence data can be gathered for the candidate.
			 */

			LOG.debug("Skipped candidate " + surfaceFormOccurrence.surfaceForm());
		}

		if (candidateData != null) {

			List<TaggedToken> leftContext = null;
			try {
				leftContext = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getLeftContext(surfaceFormOccurrence, 2);
			} catch (ItemNotFoundException ignored) {}

			CandidateData left1 = null;
			if(leftContext.size() > 0) {
				try {
					left1 = dataProvider.getCandidateData(leftContext.get(0).getToken());
				} catch (ItemNotFoundException ignored) {}
			}

			CandidateData left2 = null;
			if(leftContext.size() > 1) {
				try {
					left2 = dataProvider.getCandidateData(leftContext.get(1).getToken());
				} catch (ItemNotFoundException ignored) {}
			}

			List<TaggedToken> rightContext = null;
			try {
				rightContext = ((TaggedText) surfaceFormOccurrence.context()).taggedTokenProvider().getRightContext(surfaceFormOccurrence, 2);
			} catch (ItemNotFoundException ignored) {}

			CandidateData right1 = null;
			if(rightContext.size() > 0) {
				try {
					right1 = dataProvider.getCandidateData(rightContext.get(0).getToken());
				} catch (ItemNotFoundException ignored) {}
			}

			CandidateData right2 = null;
			if(rightContext.size() > 1) {
				try {
					right2 = dataProvider.getCandidateData(rightContext.get(1).getToken());
				} catch (ItemNotFoundException ignored) {}
			}


			try{
				if(candidateData.getCountCorpus() != null && candidateData.getCountCorpus() < this.unigramCorpusMax)
					instance.setValue(unigram_count_corpus, candidateData.getCountCorpus());
				//else
					//instance.setValue(i(unigram_count_corpus, buildAttributeList()), this.unigramCorpusMax);
			}catch (ArrayIndexOutOfBoundsException ignored) {}

			try {
				if(candidateData.getCountWeb() != null && candidateData.getCountWeb() > this.unigramWebMin)
					instance.setValue(i(unigram_count_web, buildAttributeList()), candidateData.getCountWeb());
			}catch (ArrayIndexOutOfBoundsException ignored) {}


			/**
			 * Co-Occurrence data of the left neighbour token:
			 */

			if(left1 != null && !leftContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN) && !leftContext.get(0).getPOSTag().contains("$") && !leftContext.get(0).getPOSTag().equals("in")) {

				try {
					CoOccurrenceData leftBigram = dataProvider.getBigramData(left1, candidateData);

					if(leftBigram.getUnitCountWeb() > this.bigramLeftWebMin) {

						try {
							instance.setValue(i(bigram_left_count_corpus, buildAttributeList()), leftBigram.getUnitCountCorpus());
						}catch (ArrayIndexOutOfBoundsException ignored) {}

						try{
							instance.setValue(i(bigram_left_significance_web, buildAttributeList()), leftBigram.getUnitSignificanceWeb());
						}catch (ArrayIndexOutOfBoundsException ignored) {}


						try {
							instance.setValue(i(bigram_left_count_web, buildAttributeList()), leftBigram.getUnitCountWeb());
						}catch (ArrayIndexOutOfBoundsException ignored) {}

						try {
							instance.setValue(i(bigram_left_significance_corpus, buildAttributeList()), leftBigram.getUnitSignificanceCorpus());
						}catch (ArrayIndexOutOfBoundsException ignored) {}
					}

				} catch (ItemNotFoundException ignored) {}


			}


			/**
			 * Co-Occurrence data of the left two tokens
			 */
			if(left1 != null && left2 != null) {

				try {
					CoOccurrenceData leftTrigram = dataProvider.getTrigramData(left2, left1, candidateData);
					if(!(leftContext.get(0).getPOSTag().equals(",") || leftContext.get(1).getPOSTag().equals(",")) &&
							!(leftContext.get(0).getPOSTag().equals("in") && leftContext.get(1).getPOSTag().equals("at"))
							&& leftTrigram.getUnitCountWeb() >= this.trigramLeftWebMin
							)
						instance.setValue(i(trigram_left_count_web, buildAttributeList()), leftTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException ignored) {}
				catch (ItemNotFoundException ignored) {}

			}


			/**
			 * Co-Occurrence data of the right two tokens
			 */

			if(right1 != null && right2 != null) {

				try{
					CoOccurrenceData rightTrigram = dataProvider.getTrigramData(candidateData, right1, right2);

					if(!(rightContext.get(0).getPOSTag().equals(",") || rightContext.get(1).getPOSTag().equals(","))
						&& rightTrigram.getUnitCountWeb() >= this.trigramRightWebMin)
						instance.setValue(i(trigram_right_count_web, buildAttributeList()), rightTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException ignored) { }
				catch (ItemNotFoundException ignored) { }
			}




			/**
			 * Co-Occurrence data with term in the middle
			 */

			if(left1 != null && right1 != null) {
				try{
					CoOccurrenceData middleTrigram = dataProvider.getTrigramData(left1, candidateData, right1);
					if(!(leftContext.get(0).getPOSTag().equals(",") || rightContext.get(0).getPOSTag().equals(","))
							&& !(leftContext.get(0).getPOSTag().equals("in") || rightContext.get(0).getPOSTag().equals("cc"))
							&& middleTrigram.getUnitCountWeb() >= this.trigramMiddleWebMin
							)
						instance.setValue(i(trigram_middle_count_web, buildAttributeList()), middleTrigram.getUnitCountWeb());
				}
				catch (ArrayIndexOutOfBoundsException ignored) { }
				catch (ItemNotFoundException ignored) { }
			}



			/**
			 * Co-Occurrence data of the right neighbour token:
			 */

			if(right1 != null && !rightContext.get(0).getPOSTag().matches(FUNCTION_WORD_PATTERN)) {

				CoOccurrenceData rightNeighbourData = null;
				try {
					rightNeighbourData = dataProvider.getBigramData(candidateData, right1);
				} catch (ItemNotFoundException e) {
					//No right neighbour token found or no data for the token
				}

				if (rightNeighbourData != null && rightNeighbourData.getUnitCountWeb() > this.bigramRightWebMin) {

					try {
						instance.setValue(i(bigram_right_count_web, buildAttributeList()), rightNeighbourData.getUnitCountWeb());
					}catch (ArrayIndexOutOfBoundsException ignored) {}

				}
			}
		}

		try {
			int uppercaseValue = CandidateFeatures.nonSentenceInitialUppercase(surfaceFormOccurrence);
			instance.setValue(i(non_sentence_initial_uppercase, buildAttributeList()), uppercaseValue);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			int quotedValue = CandidateFeatures.quoted(surfaceFormOccurrence);
			instance.setValue(i(quoted, buildAttributeList()), quotedValue);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}

		try{
			int nextToUppercase = CandidateFeatures.nextToUppercase(surfaceFormOccurrence);
			instance.setValue(i(next_to_uppercase, buildAttributeList()), nextToUppercase);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}


		try {
			Integer prePOS = CandidateFeatures.prePOS(surfaceFormOccurrence);
			if (prePOS != null)
				instance.setValue(i(pre_pos, buildAttributeList()), prePOS);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}


		try{
			Integer nextPOS = CandidateFeatures.nextPOS(surfaceFormOccurrence);
			if (nextPOS != null)
				instance.setValue(i(next_pos, buildAttributeList()), nextPOS);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}


		try{
			if (CandidateFeatures.isInEnumeration(surfaceFormOccurrence))
				instance.setValue(i(in_enumeration, buildAttributeList()), 0);
		}catch (ArrayIndexOutOfBoundsException e) {
			//value does not exist in header: ignore
		}


		try{
			if(CandidateFeatures.isPossessive(surfaceFormOccurrence))
				instance.setValue(i(possesive, attributeList), 0);
		}catch (ArrayIndexOutOfBoundsException ignore) {}


		if (verboseMode)
			explain(surfaceFormOccurrence, instance);

		return instance;
	}

}
