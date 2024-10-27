# CSCI 576 - Multimedia Systems Design

Implementation of assigments for coursework in CSCI576 - Multimedia Systems Design

## Assignment 1

- Implement image scaling to understand the effect of sampling
- Implementing scaling down from 4xHD (7680x4320) by a factor of S (0 - 1)
- Implementing antialiasing if set to true (1)
- Implementing window to view original image on click of control key and mouse movement
- [Code](<Assignment 1/Java Code/ImageDisplay.java>)

## Assignment 2

- Implement object matching to understand colour theory
- Implementing colour conversion from RGB to HSV space
- Implementing colour matching using Hue to obtain object boundaries using BFS and Number of Islands problem
- Filtering bounding boxes using Euclidean distances
- [Code](<Assignment 2/Java Code/ImageDisplay.java>)

## Assignment 3

- Implement image compression using Wavelet theory
- Implemnting scaling down images using wavelet theory building the reduced image using average and differnce of two pixels
- Implementing compression i.e throwing away high frequency data
- Implementing reconstruction of image using wavelet theory builing the image using sum and difference
- [Code](<Assignment 3/Java Code/ImageDisplay.java>)

## Project

- Repository - https://github.com/jugal13/Video-Search-Indexing
- Problem statement
  - Provided a database of videos and given a set of query videos, find an exact video match and compute the indexed start frame of the query video in the matched video
  - Constraints
    - Each frame size is fixed - CIF format (352x288)
    - Frame rate is fixed (30fps)
    - Each frame is a perfect match perceptually and at a pixel level i.e at a bit level
    - Query videos will be of a length between 20 to 40 seconds
    - Database videos will be upwards of 10 minutes
