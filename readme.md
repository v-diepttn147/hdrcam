# Lowlight Image Enhancement

This is a repo for Android Application for Lowlight Image Enhancement.

## Introduction

### Enhancement method

The model architecture was adopted from [hdrnet](https://github.com/google/hdrnet), which ultilize the bilateral upsampling to speed up model inference time. See the below figure for the illustration of model architecture.

![hdrnet model architect](lowlightapp/src/main/assets/hdrnet.jpg)

#### Modifications

- Loss functions: extra unsupervised loss functions are added in order to generalize the model.
- Dataset: model was retrained on the modified [MIT-Adobe5K Dataset](https://people.csail.mit.edu/vladb/photoadjust/db_imageadjust.pdf)

### Requirements

```
- Android studio
- SDK 29
- NDK 21
```

## Results

### Application architecture

![Data flow in application](lowlightapp/src/main/assets/ll-sdk-flow-sketch.png)

### Application UI

|                     Before                      |                     After                      |
| :---------------------------------------------: | :--------------------------------------------: |
| ![Before](lowlightapp/src/main/assets/ui-1.png) | ![After](lowlightapp/src/main/assets/ui-2.png) |

## Benchmark

### Comparing to other methods

![FIQA](lowlightapp/src/main/assets/fiqa.png)

![NIQA](lowlightapp/src/main/assets/niqa.png)

### Samples visualization

![Sample 1](lowlightapp/src/main/assets/sample1.png)

![Sample 2](lowlightapp/src/main/assets/sample2.png)

![Sample 3](lowlightapp/src/main/assets/sample3.png)
