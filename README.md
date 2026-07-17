# Multithreaded Transaction Tallier

A Java command-line tool that parses a factory's transaction log (up to 100,000+ entries)
and produces a report of total dollar amount and per-item quantities, using a configurable
number of worker threads to process the file in parallel.

## What it does

Given a transaction log file where each line looks like:

```
buy copper wires 240 @ $1
sell advanced circuits 10 @ $250
```

the program reports:

```
Transaction total is $1505

Categories and their quantities:
copper wires: 240
advanced circuits: 10
...
```

## Architecture

- **`TransactionReader`** — reads the file into an in-memory list of raw transaction lines.
- **`TransactionTallier`** — shared state accessed by all worker threads: the remaining
  transaction queue, the running dollar total, and a `HashMap` of item → quantity. All
  methods that mutate shared state (`getNextTransaction`, `addToTotal`, `updateCategories`)
  are `synchronized` to prevent race conditions between threads.
- **`TallyWorker`** — implements `Runnable`. Each worker repeatedly pulls the next
  transaction off the shared queue and updates the tallier, until the queue is empty.
- **`Main`** — drives the program in one of two modes (see below).

## Running it

Compile:
```
javac -d out src/*.java
```

Interactive mode (same flow as reading a file, choosing a thread count, and printing a report):
```
java -cp out Main
```

Benchmark mode (times the same file processed with 1, 2, 4, 8, 16, 32, and 64 threads back to back):
```
java -cp out Main --benchmark transactions.txt
```

## Why multithreading here — and what I found benchmarking it

The transaction list is a shared queue: any thread can safely pull the next line and update
the running total/category map, since all of that shared state is protected by
`synchronized` methods. That makes this a natural fit for a simple work-stealing pattern
with N worker threads.

Run `--benchmark` yourself and fill in your numbers below — timings depend on your machine,
but the *shape* of the result is the interesting part: throughput improves with more threads
up to a point, then flattens out (or even degrades) past a certain thread count, since the
`synchronized` methods on `TransactionTallier` serialize access to shared state and
eventually become the bottleneck rather than the parsing work itself. That's a real,
explainable tradeoff between parallelism and lock contention — the kind of thing worth
discussing in an interview about this project.

| Threads | Time (ms) | Txns/sec |
|--------:|----------:|---------:|
| 1       |           |          |
| 2       |           |          |
| 4       |           |          |
| 8       |           |          |
| 16      |           |          |
| 32      |           |          |
| 64      |           |          |

## What this project demonstrates

- Multithreading in Java (`Runnable`, `Thread`, `join`)
- Thread-safe shared state with `synchronized` methods
- Parsing and validating semi-structured text input
- Basic performance benchmarking and reasoning about concurrency tradeoffs (lock contention vs. parallelism)
