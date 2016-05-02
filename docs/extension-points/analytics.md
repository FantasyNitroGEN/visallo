# Analytics

## Complete Dataset Analytics

Complete dataset analytics are analytics which you would like to analyze all of the data in the system as a whole. Example of these analytics are classification, clustering, recommendation, and statistical algorithms. Please consider our commercial Visallo Enterprise distribution for large dataset analytics as it is designed for such a use case.

## Individual Scoring

Individual scoring analytics are useful when you are working on a single element or an element and the surrounding elements. For this we recommend using a [graph property worker (GPW)](https://github.com/v5analytics/visallo/blob/master/core/core/src/main/java/org/visallo/core/ingest/graphProperty/GraphPropertyWorker.java). Graph property workers will get notified of every change made to elements in the system and allow you to act on those changes.

There are many examples of graph property workers in the open source Visallo project. You can find some of them [here](https://github.com/v5analytics/visallo/search?q=%22extends+GraphPropertyWorker%22&type=Code)

### Example

Imagine we wanted to update a fraud score on a person vertex. We could write a GPW which listens for any changes made to any person vertex and update that fraud score. Below is some pseudo code on how you might do that.

```java
public class PersonFraudScoreGPW extends GraphPropertyWorker {
  public boolean isHandled(Element element, Property property) {
    return isPersonVertex(element);
  }

  public void execute(InputStream in, GraphPropertyWorkData data) throws Exception {
    double fraudScore = calculateFraudScore(data.getElement(), data.getElement().getEdges());
    data.getElement().setProperty("fraudScore", fraudScore);
  }
}
```
