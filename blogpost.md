# Custom Lucene TokenFilters for Solr

[Apache Solr](http://lucene.apache.org/solr/) is a great search platform.
It is based on [Lucene](http://lucene.apache.org/core/), and already does most of the things you are likely to need out of the box.
Sometimes, however, you need to get your hands dirty in order to get things Just Right™.

In this blogpost we will show how to implement a custom `TokenFilter` in Lucene.
This can be used for a number of things, but our specific example will be how to implement a custom phonetic search.

This post won't be a complete beginners introduction to Solr or Lucene, so some familiarity is expected.
However, you shouldn't need to know much more than you would from playing with the [Solr documentation example](http://lucene.apache.org/solr/api-4_0_0-BETA/doc-files/tutorial.html). 
If you haven't used solr before, or is interested in our example data, we have bundled an instance with the data used in this example.


## Use Case: Phonetic Search

A phonetic algorithm is used to index a given word based on its pronunciation. It takes a word as input, and converts this to a digest representing the way the word sounds when spoken.
Words sounding roughly the same should thus be represented the same way by the algorithm, and when searching words sounding like the query should match its phonetic digest.  

Solr already supports four different phonetic algorithms: [Soundex](http://en.wikipedia.org/wiki/Soundex), RefinedSoundex, [Metaphone](http://en.wikipedia.org/wiki/Metaphone) and DoubleMetaphone.
For many practical purposes, one of these four will suffice.
However, phonetic algorithms are implemented based on a language's linguistics and thus, for some situations, such as when searching for norwegian names, these four just dosen't cut it.

In order to use a custom phonetic algorithm, we will implement a TokenFilter for this purpose.


## The Fields and Field Types

The documents and their fields, as well as the field types, are defined in our [schema.xml](https://github.com/kvalle/norphoname/blob/master/schema.xml) file.

In our example, the documents represent persons, with names and birth dates:

	<fields>
		<field name="id" type="string" indexed="true" stored="true" />
		<field name="name" type="text_general" indexed="true" stored="true" />
		<field name="name_phonetic" type="phonetic" indexed="true" stored="false" />
		<field name="birth_date" type="string" indexed="true" stored="false" />
	</fields>

The field called `name_phonetic` is where we'll store our phonetic encoding of the names.
This field does not need to be specified when documents are inserted into the collection, but will be copied and transformed from the `name` field automatically.

	<copyField source="name" dest="name_phonetic" />

The field type `phonetic` isn't one of the default types, but must be defined by us as well:

	<fieldtype name="phonetic" stored="false" indexed="true" class="solr.TextField">
	    <analyzer>
	        <tokenizer class="solr.WhitespaceTokenizerFactory"/>
	        <filter class="solr.WordDelimiterFilterFactory" generateWordParts="1" catenateWords="1" />
	        <filter class="solr.LowerCaseFilterFactory" />
	        <filter class="org.example.search.NorwegianNamesFilterFactory" />
	    </analyzer>
	</fieldtype>

When a name is copied from the `name` field and passed through the tokenizer and filters specified under `analyzer`.
The result of this is then stored in the index.

The tokenizer simply splits names into different tokens using whitespace.
The first filter splits tokens further if they contain word delimiters, e.g. "Bob-Kåre" results in "Bob" and "Kåre", and the lowercase filter makes the search case insensitive.

The last filter, however, is one we'll need to provide.


## The FilterFactory

*TODO: discuss the filterfactories specified in the analyzer filter chain, and describe the implementation.*

*Hook from solr into the filter factory*

Responsible for instantiating a filter.

## The Filter

*TODO: discuss the actual implementation of the filter class.*

The class extends the TokenFilter class in lucene and we override the `increaseToken()` method. 
This method handles the stream of tokens, in our case, letters are processed based on their phonetic meaning and alignment. 
Our phonetic algorithm is defined in `Norphone.java`. This class holds a set of phonetic 'rules' that describe and contains grammatical characterizations for the norwegian language. Below is an excerpt from our filter class. 

	if (input.incrementToken()) {
		String name = new String(termAttribute.buffer()).substring(0, termAttribute.length());
		String phoneticRepresentation = Norphone.encode(name);

		setBuffer(phoneticRepresentation);
		termAttribute.setLength(phoneticRepresentation.length());
		return true;
	}

For each input name, we take the whole name and encode it using the norphone phonetic class. Since a phonetic algorithm is sensitive for letter alignment, we encode the whole name in one operation. The result from the encoder is the actual phonetic representation of the name. When the encoding is done, we put the phonetic representation in the `termAttribute` buffer. The buffer is returned out of the filter and stored.



## Make the new field searchable

In Solr, incomming queries are handled by different `RequestHandlers`.
These are specified and configured in the [solrconfig.xml]() file.
In order for our new `name_phonetic` field to be useful, we need to tell Solr to match queries against it.

Below is an excerpt from `solrconfig.xml` showing the configuration of a pretty standard [SearchHandler](http://wiki.apache.org/solr/SearchHandler).

	<requestHandler name="/select" class="solr.SearchHandler">
	<lst name="defaults">
	    <str name="echoParams">explicit</str>
	    <int name="rows">10</int>
	    <str name="df">name</str>
	 <str name="defType">dismax</str>
	    <str name="qf">name^3 name_phonetic</str>
	    <str name="q.alt">*:*</str>
	    <str name="fl">*,score</str>
	</lst>
	</requestHandler>

The handler is configured to use the [dismax query mode](http://wiki.apache.org/solr/DisMax).
The essential line here is the setting of the `qf` property.
`qf` stands for *query fields*, and tells Solr which fields to match the query against.
Also notice the `^3` used to *boost* the normal name search, i.e. telling Solr that an exact match is three times more important than a phonetic match.

Now, with the field created, implemented and activated, we are ready to start using it.


## The Result

*TODO: present an example query with results to show the search working, and refer to the working example on github.*
