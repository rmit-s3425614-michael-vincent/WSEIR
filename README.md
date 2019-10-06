s3425614 - Michael Vincent

s3703700 - Zaed Ahmed


Compile and run with file location as current working directory.


# Compile

javac -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar *.java


# Run

java -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar index [-s (stoplist file)] \[-p] (document file)

java -cp .:lib/jopt-simple-5.0.2.jar:lib/jsoup-1.12.1.jar search --(retrieval-function-1) [--(retrieval-function-2)] -q (query-label) -n (num-results) -l (lexicon file) -i (invlists file) -m (map file) [-s (stoplist file)] (queryterm-1) [(queryterm-2)... (queryterm-N)]

Choice of retrieval function are BM25 or MMR.
