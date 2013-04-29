
A Storm project experiment with VowPal wabbit and the twitter streaming api

## Usage

Be sure to read up on Storm first:

http://storm-project.net/
https://github.com/nathanmarz/storm/
https://github.com/nathanmarz/storm/wiki

This is my storm playground experimenting with Machine learning in Storm with VowPal wabbit. 

One bolt predicts sentiment using vowpal wabbit logistic regression named sent.model and another computes if the user is a bot or human using some arbitrary calculation (note this is just an experiment in making bolts compute several functions at once rather than an accurate way of measuring of bots)


the aclImdb corpus is used, the original training and test data can be found in ~/storm-ml-play/vwtraining/aclImdb.tar.gz

A quick and dirty script turned the original training data into an input that vowPal wabbit will accept, see ~/storm-ml-play/vwtraining/train.vw and test.vw for the training and test data.  the sentiment analysis model can be found ~/storm-ml-play/vwtraining/sent.model

*** I know there are a lot of bolts but I am playing around with large vs small functions to see if it makes a difference to storm's speed.

To run on a local cluster:

```bash
lein run -m storm-ml-play.topology/run!
# OR
lein run -m storm-ml-play.topology/run! debug false workers 10
```

To run on a distributed cluster:

```bash
lein uberjar
# copy jar to nimbus, and then on nimbus:
bin/storm jar path/to/uberjar.jar storm-ml-play.TopologySubmitter workers 30 debug false
```

or use `[storm-deploy](https://github.com/nathanmarz/storm-deploy/wiki)`

## License

Copyright © 2013 Yodit Stanton 

Distributed under the Eclipse Public License, the same as Clojure.
=======
storm-ml-play
===========
>>>>>>> bfd8d749302d344b404a122e7205fce79a879958
