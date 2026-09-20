---
layout: post
title: "You probably don’t need an LLM to classify Czech municipal documents"
date: 2026-09-20
categories: [machine learning, nlp, text classification, czech, ai]
excerpt: "How I ended up using a small Czech encoder, active learning and 733 manually curated examples instead of a generative LLM to classify municipal resolutions."
toc: true
---

## Contents
{: .no_toc }

* TOC
{:toc}

## From LLMs to a Small Specialized Classifier

While working on a project that processes resolutions published by the City Council and Municipal Assembly of Litovel, I ran into a fairly simple requirement: I wanted to automatically assign each resolution to a thematic category.

For example:

- sale of municipal land → `property`
- road reconstruction → `transport`
- budget amendment → `finance`
- sewer infrastructure → `infrastructure`
- grant for a sports club → `sport`

At first glance, this looks exactly like the kind of problem for which people now reach for an LLM.

I eventually ended up somewhere quite different: with a small Czech encoder model of roughly 13.5 million parameters, 733 manually curated examples, and a classifier small enough to run locally or directly in GitHub Actions.

## First attempt: zero-shot classification

The first experiment was straightforward.

Give the model the text of a resolution together with a list of categories and ask it to choose one.

No custom training dataset. No fine-tuning. Appraently this is called **zero-shot classification**.

I experimented with Needle 3, a small generative model. The idea was attractive: the model was tiny, fast, and potentially suitable for running directly in CI.

In practice, the results were not good enough.

The problem is that the model does not merely need to understand Czech. It also needs to understand the rather specific language of municipal resolutions and, more importantly, what exactly my categories mean.

Consider something like:

> The City Council approves an amendment to an agreement on a future easement for an engineering network.

Is that `property`, because the resolution is about an easement?

Is it `infrastructure`, because the easement is related to a technical network?

Or something else?

Without explicit rules, even a human may reasonably disagree.

Zero-shot classification therefore turned out to be a poor fit for this particular task. At least with the model I tried. That may well change as small language models improve.

## Using a classifier instead of an LLM

The next experiment used `ufal/eleczech-lc-small`, a small Czech language encoder.

The difference compared with a generative LLM is important.

A generative model effectively does something like:

```text
text
↓
generate tokens
↓
"I think this belongs to the transport category."
```

A classifier only needs to do:

```text
text
↓
language representation
↓
14 scores
↓
pick the highest one
```

It does not need to generate anything since task is tightly constrained: given one text, choose one of fourteen known classes. That is exactly the sort of problem an encoder classifier is designed for.

### But now we need labeled data

A pretrained EleCzech model understands Czech, but it knows nothing about my categories.

I need to show it examples:

```text
"approves the sale of municipal land..."       → property
"reconstruction of a local road..."            → transport
"approves a budget amendment..."               → finance
```

This is **supervised learning**. Each training text has a label that we consider correct. That reference answer is commonly called **ground truth**.

For this type of problem, however, ground truth is not some absolute physical truth. It is better understood as:

> the authoritative human label according to a predefined taxonomy.

And that is where the project became more interesting...

#### The hardest part was not the model, but the taxonomy

At first I had a list of categories. Very quickly, it became obvious that a list was not enough.

For example:

> Lease of municipal land for the purpose of holding a cultural event.

Should that be `property` or `culture`?

Or:

> Grant to a sports club.

Is that `finance`, because money is involved, or `sport`?

I needed consistent rules.

The resulting taxonomy contains fourteen categories:

```text
administration
culture
education
environment
finance
infrastructure
planning
property
security
social
sport
strategy
transport
waste
```

The main principle became:

> Classify the primary substantive domain, not the legal or financial mechanism.

So a grant to a sports club is `sport`. A sale or lease of municipal land is usually `property`. A statutory land-use plan is `planning`, while a general municipal development strategy is `strategy`. Municipal police and volunteer fire brigades belong to `security`.

Without rules like these, the training dataset would contain contradictory examples, and expecting consistent behavior from the model would be unreasonable.

### The first results looked suspiciously good

After creating the first reasonably sized dataset, I randomly split it into train, validation and test sets.

* The model learns from `train`.
* The `validation` set is used during development, for example to choose the best checkpoint.
* The `test` set is supposed to remain untouched until the end.

The result was roughly 93% accuracy. That looked excellent. But municipal resolutions are highly repetitive. A resolution from 2024 can look almost identical to one from 2025:

> The City Council approves the conclusion of an agreement establishing an easement...

With a random split, nearly idaentical templates can easily appear in both training and test data. The model may therefore appear smarter than it really is.

### Temporal split: better, but not leakage-proof

To get an evaluation that better matched the intended deployment scenario, I tried a chronological split:

```text
2022–2024 → train
2025      → validation
2026      → test
```

The idea is simple:

> learn from older resolutions and classify newer ones.

Accuracy dropped to roughly 85%. That result was useful, but there is an important caveat. A temporal split does **not** automatically eliminate template leakage.

As I wrote before: municipal resolutions often repeat the same legal and administrative patterns year after year. For example, the training set might contain:

```text
2024:
"The City Council approves the conclusion of an easement agreement..."
```

while the test set contains:

```text
2026:
"The City Council approves the conclusion of an easement agreement..."
```

The parcel number, organization or amount may be different, but structurally the texts can be almost identical.

So chronological separation answers a useful question:

> How well does a model trained on older Litovel resolutions classify later Litovel resolutions?

But it does not necessarily answer a stricter question:

> How well does the model generalize to wording or decision patterns it has never seen before?

Those are two different things.

### A stricter test would group similar templates

If the goal were specifically to measure generalization to unseen patterns, a better retrospective evaluation would be to identify near-duplicate or template families and keep each family entirely on one side of the split.

Conceptually:

```text
normalize documents
↓
detect similar templates
↓
group near-duplicates
↓
keep each group entirely in train or test
```

That would prevent the model from learning one version of a recurring template and being tested on an almost identical version. Ideally, such grouping could also be combined with temporal separation.

But there is a trade-off: Recurring templates are genuinely part of the production environment. If the City of Litovel publishes the same type of easement resolution every year, recognizing that recurring pattern is legitimate production performance. Removing all such similarity from the test set measures a different and deliberately harder problem.

So there are really at least two useful evaluation questions:

```text
1. How well does the model classify future Litovel resolutions?
   → temporal or prospective evaluation

2. How well does it generalize to previously unseen semantic or template patterns?
   → group/template-aware evaluation
```

One metric should not be mistaken for the other. At this point I could simply have labeled several hundred more random resolutions. But the model already handled many of them easily. For example:

```text
property 0.99
finance  0.002
...
```

Another example like that adds relatively little information. This is much more interesting:

```text
transport       0.44
infrastructure  0.41
```

Here the model is clearly uncertain. That led to **active learning**.

## Active learning

The idea is simple:

1. classify all currently unlabeled data,
2. identify cases where the model is uncertain,
3. label those cases manually,
4. add them to the training dataset,
5. retrain the model.

Instead of spending human effort randomly, we deliberately spend it where the model can learn the most. One useful selection signal was the difference between the first and second highest scores: the **margin**.

For example:

```text
sport    0.52
culture  0.46
```

has a margin of:

```text
0.52 - 0.46 = 0.06
```

That is a highly uncertain prediction. On the other hand:

```text
sport    0.97
culture  0.01
```

has a very large margin. Another manually labeled example of the same type is unlikely to add much value.

### Low accuracy on active-learning batches was a good sign

Some active-learning rounds produced only 30-50% accuracy on the selected examples.

At first glance that sounds terrible. In reality, it is exactly what we want. These were not random representative samples. We deliberately selected the examples where the model was most likely to fail.

If the model achieved 99% accuracy on an active-learning batch, that would more likely indicate that we were wasting human effort labeling examples it already understood.

### Softmax scores are not probabilities of being correct

The classifier returns a score for every category. After applying softmax, we might see something like:

```text
transport       0.91
infrastructure  0.04
property        0.02
...
```

It is tempting to say:

> The model is 91% confident.

That is not necessarily correct.

A softmax score is not automatically a calibrated probability that the prediction is correct. During development I found examples where the model was highly confident and still wrong. For that reason, I also reviewed some very high-confidence predictions.

These **confident controls** helped uncover systematic mistakes that pure uncertainty sampling would never find.

### Then I broke the production model

One of the most useful failures happened during production training.

My initial idea was:

1. train using a train/validation split,
2. determine the optimal number of epochs,
3. create a fresh model,
4. train that fresh model on all labeled data for the same number of epochs.

This sounds reasonable but the classification head is randomly initialized whenever a fresh model is created.

The optimal number of epochs for one randomly initialized head is not necessarily optimal for another.

The result: After classifying all 2,942 resolutions, the distribution contained:

```text
environment: 1
```

That looked very suspicious and after checking the model against known labeled data revealed something much worse:

> Out of all 16 known `environment` examples, the model classified exactly zero correctly.

Almost all of them ended up as `waste` with the overall accuracy still high enough that this failure was not immediately obvious.

### Training-set fit as a sanity check

From that point on, I added another check after final production training:

> Can the final model at least reproduce the examples it has just been trained on?

This is **not** a generalization metric.

If a model scores 99.5% on its training set, that does not mean it will achieve 99.5% accuracy in production, it is simply a sanity check for the training pipeline.

If I have just trained the model on all known `environment` examples and its recall on that class is zero, something is obviously broken.

The final training procedure therefore became:

```text
train + validation
       ↓
select the best checkpoint
       ↓
continue FROM THAT SAME checkpoint
       ↓
train on all labeled data with a lower learning rate
       ↓
fit check
```

No fresh randomly initialized classifier is created in the second stage.

### 733 manually curated examples

After several active-learning rounds, the dataset reached 733 manually labeled resolutions. Ambiguous examples were deliberately excluded.

This is important because single resolution may combine, for example, a cultural event, a volunteer fire brigade and a budget amendment.

If I cannot assign one primary category consistently according to my own taxonomy, it is better not to use that resolution as a training example than to teach the model an arbitrary choice.

The final frozen state became:

```text
taxonomy v7
gold-v1: 733 examples
model v1.0.0
```

At that point I stopped development.

### Why stop training?

It would be easy to continue indefinitely. Find another mistake, label it, retrain, find another mistake, and repeat. But then the same historical corpus would increasingly influence not only training, but also taxonomy design and development decisions.

Any reported accuracy on that corpus would become less and less meaningful as an estimate of behavior on genuinely new data. So the model is now frozen.

The next useful test starts when new resolutions are published.

## Prospective evaluation

This will answer the most important production question. Model `v1.0.0` already exists and future municipal resolutions do not.

When new documents are published:

```text
new resolution
      ↓
frozen model v1.0.0
      ↓
prediction
      ↓
independent human labeling
      ↓
compare against ground truth
```

Those documents could not have influenced any:

- training,
- active learning,
- taxonomy design,
- checkpoint selection.

This avoids the development feedback loop affecting the evaluation set. However, even prospective evaluation does not magically remove recurring templates.

A future resolution may still closely resemble resolutions from previous years and that is not necessarily a problem: if the goal is to measure real production performance on Litovel's future resolutions, recurring administrative patterns are part of the real workload.

Prospective evaluation therefore gives the cleanest answer to:

> How well does frozen model `v1.0.0` classify resolutions that are published after its development?

A separate template-aware benchmark would be needed to answer the stricter question of how well it handles genuinely novel patterns.

## What I learned

The hardest part of the project was not choosing the neural network architecture.

The most valuable parts turned out to be:

- a clearly defined taxonomy,
- consistent human labels,
- examples close to decision boundaries,
- a reproducible training pipeline,
- understanding what each evaluation setup actually measures,
- separating training-fit metrics from real evaluation.

And one broader lesson is fairly simple:

> Not every NLP problem needs a generative LLM.

If the output is one of a small number of known categories, a small specialized encoder can be faster, cheaper, easier to audit, and simply better suited to the task. In this case, the entire model is only around 50 megabytes and inference can run directly inside GitHub Actions without calling an external AI API.

Perhaps the most interesting part is that the model itself is now one of the easiest components to replace.

If licensing or quality requirements eventually push me toward another Czech or multilingual encoder, the expensive part of the work is already done: 733 human decisions defining what the correct answer actually means.

## Resources

* <https://github.com/honza-kasik/litovel-resolution-classifier>