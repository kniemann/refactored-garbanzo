# refactored-garbanzo
This is a capstone project for UW Scala programming certification.

It performs image recognition using the Java API for Tensorflow. Images can be published via a REST API, file upload and web form. Images are routed through a Kafka topic and consumer by scalable consumers that asychronously label the images and then write the result to a Cassandra cluster. A web UI is provided for viewing the labeled images.
