# MeshSim

A simulation framework for simulating mesh messaging systems. This repository holds the specific simulations for [Anix](https://cs.uwaterloo.ca/~s4kamali/paperfiles/kamali-sp25.pdf) and [Rangzen](https://arxiv.org/pdf/1612.03371). The features of this simulator include:

* capacity to simulate any mesh messaging system with any communication model
* easily tunable settings for every aspect of the simulation
* an in-memory data collector
* different awareness settings for users

Our next update will focus on having individual users with various awareness settings (as opposed to the whole population).

## How to use

First, install the requirements by running:

    pip install -r requirements.txt

Then, simply run the project by using:

    python main.py
