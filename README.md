# DEMA: a distance-bounded energy-field minimization algorithm to model and layout biomolecular networks with quantitative properties

## Authors:
Zhenyu Weng, Zongliang Yue, Yuesheng Zhu*, Jake Yue Chen*

### Affiliations:
1. Communication and Information Security Lab, Institute of Big Data Technologies, Shenzhen Graduate School, Peking University, China
2. Informatics Institute, School of Medicine, University of Alabama at Birmingham, USA

## Contact:
* jakechen@uab.edu
* zhuys@pku.edu.cn

## Tutorial Video:
*   https://youtu.be/N9EhWbZGSg4

## Overview / What is DEMA?

DEMA (Distance-bounded Energy-field Minimization Algorithm) is a computational tool developed by researchers from Peking University and the University of Alabama at Birmingham. It is designed for modeling and visualizing biomolecular networks. Biomolecular networks are complex systems of interacting molecules within a cell (e.g., proteins, genes, and metabolites) that govern cellular processes.

The core of DEMA lies in its "distance-bounded energy-field minimization" approach. In simple terms, this method treats molecules and their interactions like a system of interconnected springs. The algorithm then tries to arrange these springs to minimize the overall tension (energy) in the system, while also respecting specified distance constraints between particular springs. This process results in an optimal layout of the biomolecular network, making its structure and molecular relationships easier to understand.

A key feature of DEMA is its ability to incorporate "quantitative properties" into the network model. This means that the algorithm can consider measurable characteristics of the molecules and their interactions, such as reaction rates, concentration levels, or binding strengths. By including these quantitative aspects, DEMA can create more accurate and informative network layouts that reflect the dynamic nature of cellular processes.

### Main Function Points
*   Modeling and layout of biomolecular networks with quantitative properties.
*   Provides a library called "mdsj" for multidimensional scaling (related to layout algorithms).
*   Offers `sample-custom-layout-1.0.jar` as DEMA version 1.0 (the runnable plugin).

### Importance and Application
The importance of an algorithm like DEMA lies in its potential to help researchers understand complex biological systems. By providing clear and quantitatively informed visualizations of biomolecular networks, DEMA can aid in:
*   Identifying key components and pathways within a cell.
*   Understanding how cellular processes are regulated.
*   Discovering potential targets for drug development.
*   Analyzing the effects of genetic mutations or environmental changes on cellular function.

In essence, DEMA offers a sophisticated method for untangling the complexity of biomolecular networks, paving the way for new insights in various areas of biological research.

## Technology Stack
*   Java (as a Cytoscape plugin)

## How to Use

### Prerequisites

*   **Java:** As DEMA is a Cytoscape plugin (distributed as a JAR file), you will need Java installed to run Cytoscape. The specific Java version depends on your Cytoscape version. Please refer to the Cytoscape installation guide for Java compatibility.
*   **Cytoscape:** DEMA is designed as a plugin for Cytoscape. You need to have Cytoscape installed. You can download it from [cytoscape.org](https://cytoscape.org/).

### Installation/Setup

1.  **Download DEMA:** Obtain the `sample-custom-layout-1.0.jar` file. This is the DEMA plugin.
2.  **Install in Cytoscape:**
    *   Launch Cytoscape.
    *   Go to `Apps` -> `App Manager`.
    *   Click on `Install from File...`.
    *   Navigate to and select the `sample-custom-layout-1.0.jar` file.
    *   The plugin should install and will be available under the `Layout` -> `Custom Layouts` menu (usually named "DEMA1" as per the plugin's definition).

### Running DEMA

Once installed, DEMA can be applied to your network in Cytoscape:

1.  **Load your network:**
    *   Import your network data into Cytoscape. The network should define interactions between nodes. The example `network.txt` files (`plugin/data_for_basic_layout/network.txt` and `plugin/data_for_group_layout/network.txt`) show a simple two-column format representing edges (e.g., "node" and "target"). Node names in these examples are integers.
    *   **Node Attributes (Optional but Recommended):** For more control and better visualization, you can add attribute columns to your node table in Cytoscape before running DEMA. The plugin recognizes the following column names (case-sensitive):
        *   `name` (String): Node names (Cytoscape usually populates this by default upon network import).
        *   `color` (String): Specifies node colors. Supported values are "blue", "red", "green", "pink". If this column is absent or a node's color is unspecified (null), it defaults to blue.
        *   `FC` (Double): Represents "Fold Change" values, used by the layout algorithm. Defaults to 1.0 if the column is not provided.
        *   `set` (String): Used for group layouts. This column should contain comma-separated group identifiers for each node (e.g., a node in "Pathway1" and "ComplexA" would have "Pathway1,ComplexA").
    *   **Edge Attributes (Optional):** To incorporate quantitative edge properties, ensure your edge table has these columns:
        *   `interaction` (String): Describes the type of interaction (e.g., "pd", "pp"). This value is used by DEMA to correctly parse the `name` column of the edge table.
        *   `name` (String): Cytoscape's default edge name column. DEMA expects this to be formatted like: `SourceNodeName (interactionType) TargetNodeName`, (e.g., "Gene1 (pd) Gene2"). The `interactionType` within the parentheses must match a value from your `interaction` column.
        *   `IC` (Double): A quantitative property for "Internal Constraints" affecting the layout.
        *   `EC` (Double): A quantitative property for "External Constraints" affecting the layout.

2.  **Apply Layout:**
    *   With your network view active, navigate to the menu: `Layout` -> `Custom Layouts` -> `DEMA1` (or the name shown in the menu if it differs slightly).
    *   A dialog or task panel will appear with tunable parameters:
        *   **Parameter KA:** (Default: 1) - An internal energy parameter.
        *   **Parameter Kb/Ka:** (Default: Node count) - Ratio influencing other energy terms.
        *   **Parameter KC:** (Default: 0; if a "set" column is present in the Node Table, it defaults to 10 * Node count) - Influences group constraint strength.
        *   (Note: The algorithm also uses internal XRange and YRange parameters, both defaulting to 100, to define the layout's spread. These are not directly user-tunable in the basic plugin UI but determine the final coordinate scaling.)
    *   Adjust these parameters based on your network's characteristics or use the defaults for a first pass.
    *   Run the layout.

### Example Usage (Conceptual)

1.  **Prepare Input Files:**
    *   **`my_network.txt`** (Import as a network in Cytoscape):
        ```
        node    target
        A       B
        B       C
        C       A
        ```
    *   **`my_node_attributes.txt`** (Import into Cytoscape's Node Table):
        ```
        id	name	color	FC	set
        A	A	red	1.5	Group1
        B	B	blue	0.8	Group1,Group2
        C	C	green	2.1	Group2
        ```
        *(Note: Ensure node identifiers used during network creation in Cytoscape (often from the 'node' or 'target' columns of the network file) match the identifiers in your node attribute table, typically mapped to Cytoscape's 'name' or 'shared name' column.)*
    *   **`my_edge_attributes.txt`** (Import into Cytoscape's Edge Table. The 'name' column should follow the pattern "SourceNode (interactionType) TargetNode" for DEMA to parse it, and the `interaction` column should list the interaction types):
        ```
        name                    interaction     IC      EC
        A (pd) B                pd              0.5     0.2 
        B (pp) C                pp              0.8     0.9
        C (pd) A                pd              0.3     0.6
        ```

2.  **In Cytoscape:**
    *   Import `my_network.txt` to create the network structure.
    *   Import `my_node_attributes.txt` into the Node Table, ensuring attributes are correctly mapped to nodes.
    *   Import `my_edge_attributes.txt` into the Edge Table, ensuring attributes are correctly mapped to edges.
    *   Select the network.
    *   Go to `Layout` -> `Custom Layouts` -> `DEMA1`.
    *   Adjust KA, Kb/Ka, KC parameters if desired.
    *   Click "OK" or "Apply". The network will be laid out by DEMA. Nodes should be colored as specified, and the layout will reflect the FC, IC, EC, and set values if provided.

This "How to Use" section is based on the information derivable from the codebase and example files.
Refer to the tutorial video (https://youtu.be/N9EhWbZGSg4) for a visual guide and potentially more detailed examples.

## Citation
Weng Z, Yue Z, Zhu Y, Chen JY. DEMA: a distance-bounded energy-field minimization algorithm to model and layout biomolecular networks with quantitative features. Bioinformatics. 2022 Jun 24;38(Suppl 1):i359-i368. doi: 10.1093/bioinformatics/btac261. PMID: 35758816; PMCID: PMC9235497.

## Repository Structure

*   `README.md`: This document, providing an overview of DEMA, how to use it, and other relevant information.
*   `code/`: Contains the Java source code for the DEMA Cytoscape plugin.
    *   `code/internal/`: Houses the core implementation details of the DEMA algorithm and Cytoscape plugin integration. Key files include:
        *   `CyActivator.java`: Registers DEMA as a plugin within Cytoscape.
        *   `CustomLayout.java`: Defines the DEMA layout algorithm for Cytoscape.
        *   `NormalTask.java`: Manages the execution of the DEMA layout task, including parameter handling and applying visual styles.
        *   `wzylayout.java`: Likely contains the core DEMA layout logic (not directly examined but inferred from `NormalTask.java`).
*   `license.txt`: The MIT License file for this project.
*   `plugin/`: Contains the distributable plugin and example datasets.
    *   `plugin/sample-custom-layout-1.0.jar`: The runnable JAR file for the DEMA Cytoscape plugin (version 1.0).
    *   `plugin/data_for_basic_layout/`: Directory with example files for a basic network layout.
        *   `network.txt`: Example network structure.
    *   `plugin/data_for_group_layout/`: Directory with example files for a network layout with node grouping.
        *   `network.txt`: Example network structure.
        *   `node_table.txt`: Example node attributes, including "set" information for grouping.
*   `tutorial.docx`: A Microsoft Word document providing a tutorial on how to use DEMA. (Requires software capable of opening .docx files).

## Contributing

We welcome contributions to DEMA! If you're interested in helping improve the tool, here are some ways you can contribute:

### Reporting Bugs
*   If you find a bug, please check the existing issue tracker on the repository (if available) to see if it has already been reported.
*   If not, create a new issue. Please include:
    *   A clear and descriptive title.
    *   Steps to reproduce the bug.
    *   What you expected to happen.
    *   What actually happened (including any error messages).
    *   Your Cytoscape version, Java version, and operating system.
    *   Example network data (if relevant and shareable) that triggers the bug.

### Suggesting Enhancements
*   If you have an idea for a new feature or an improvement to an existing one, please open an issue to discuss it.
*   Provide a clear description of the enhancement and why it would be beneficial.

### Code Contributions
1.  **Fork the Repository:** Start by forking the project repository to your own account.
2.  **Create a Branch:** Create a new branch in your fork for your changes (e.g., `git checkout -b feature/your-feature-name` or `git checkout -b bugfix/issue-number`).
3.  **Make Changes:** Implement your feature or bugfix.
    *   **Coding Style:** Try to maintain a clean and readable code style, consistent with the existing codebase. (As this is a Java project, follow standard Java conventions).
    *   **Comments:** Add comments to your code where necessary to explain complex logic.
4.  **Test Your Changes:** Ensure your changes work as expected and do not introduce new issues. (If there are existing tests, make sure they pass. Consider adding new tests for new functionality).
5.  **Commit Your Changes:** Write clear and concise commit messages.
6.  **Push to Your Fork:** Push your changes to your forked repository (e.g., `git push origin feature/your-feature-name`).
7.  **Create a Pull Request:** Open a pull request from your branch to the main DEMA repository.
    *   Provide a clear description of the changes in your pull request.
    *   Reference any relevant issues.

### Other Ways to Contribute
*   Improve documentation (like this README or other tutorial materials).
*   Share example use cases or datasets.

Thank you for considering contributing to DEMA!

## License

This project is licensed under the MIT License.

```text
Copyright 2022 AI.MED@UAB

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
```
