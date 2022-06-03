
# RTE Extractor
![RTE_extractor](/extractor/rte-extractor.png)  
**REMEMBER**: This Extractor will ease the development of future implementations, so it's' not mandatory for script creation. However, it can be used in particular cases. 

The RTE Extractor is a component that must be embedded into a sampler.


### How to add an RTE Extractor:

![how_to_add_RTE_extractor](/extractor/add-RTE-extractor.gif)

The aim of the Extractor is to have the ability to look into response headers for a Field or a Color at given position and to set a JMeter Variable with the corresponding value.

#### Position Extraction

When the **_Next field position_** option is selected, you will be able to skip fields using the _tab offset_.  
_More information about this in the following section._

#### Tab offset cases:
 - Case 1: Value equals to one, extractor will look for the closest field forward.
 - Case 2: Value bigger than one, extractor will look for the closets field forward and skip field as many times as indicated. E.g: with an offset of two, it will search for the next field and then, skip that one to finally get the next one. 
 - Case 3: Value equals to minus one, extractor will look for the closest field backwards.
 - Case 4: Value lower than minus one, same behaviour as **_case 2_** but backwards. E.g: offset equals to minus two, extractor will look for the nearest field backwards and then skip one.  

Another possible option of extraction is to set the **_Cursor position_** as a JMeter variable.

> Note: *Field Positions* header is deprecated and will be removed in the next release. That information is now displayed on the *Segment's* response header.

Going back to the extractor, there is a mandatory field, which will contain the prefix name of your future variable:

![variable_prefix](/extractor/variable-prefix.png)

In the previous image we can visualize that the variable prefix **_position_** will have the cursor position of the current sampler. Therefore, the variable will split in two parts: (row, column). The row value will be saved in a variable called **_position_ROW_** and the column value will be **_position_COLUMN_**
 > You can check how to use JMeter variables in your tests over [here](https://jmeter.apache.org/usermanual/functions.html#top).

#### Color Extraction

This option allows to extract the hexadecimal string, representing the foreground color, at a given position.

In order to search for a color in the range of the position previously given, we added Segments header (color value and the corresponding range to every position in the screen) in Response Headers.
After providing a name and a position, the extracted string will be stored on a JMeterVariable with that name.

### Example of usage
 
 Let's put the extractor to work. In order to do that, we are going to select the option *Position Extraction*, and then *Next field position* to give a position from where to search.
 
In this example we will look for the next editable field from position (1,2). As we have seen in the response headers picture, there is one of those fields in position (1,27) which will be our target. In order to accomplish that, we must specify **tab offset** to 1.

Let's visualize all of this:

![extractor_usage](/extractor/position-extractor-usage.gif)
 > In this example we gave the extractor the beginning of a field (1,2), and as you could see, it will search for the next field on the right, even when the given position is inside a field or outside it.