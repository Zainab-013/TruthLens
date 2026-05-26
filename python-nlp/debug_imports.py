import sys
import os

# Add python-nlp to path just to see
import features
import analyzer

print("features.__file__:", features.__file__)
print("analyzer.__file__:", analyzer.__file__)
print("sys.path:")
for p in sys.path:
    print("  ", p)
