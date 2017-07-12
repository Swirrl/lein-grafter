# DEPRECATED

NOTE: This project is deprecated after grafter.pipelines was removed from
grafter and grafter.tabular.

It should still be usable in grafter versions prior to 0.8.12.

# lein-grafter

Adds some convenient commandline tools for listing and running
pipelines in a grafter project.

## Usage

Put the following into the `:plugins` vector of your grafter projects
`project.clj` e.g.

````clojure
:plugins [[lein-grafter "0.3.0"]]
````

Once this is done the following command line features will become
available:

### Listing Pipelines

Then inside a grafter pipeline that defines some pipelines with
`defpipe` and/or `defgraft` you can list pipelines like this:

    $ lein grafter list
    test-project.pipeline/convert-persons-data                   data-file                      ;; An example pipeline tabular pipeline
    test-project.pipeline/convert-persons-data-to-graph          data-file                      ;; A pipeline generating linked data

### Running Tabular Pipelines

You can run the pipelines with the command `lein grafter run`.  Pipe's
exposed with `defpipe` yield tabular output and can be used to convert
the file into csv, xls or xlsx (Excel formats) by for example running.
Note how grafter infers the format from the output files file
extension.

    $ lein grafter run test-project.pipeline/convert-persons-data ./data/example-data.csv example-output.csv
    $ lein grafter run test-project.pipeline/convert-persons-data ./data/example-data.csv example-output.xlsx
    $ lein grafter run test-project.pipeline/convert-persons-data ./data/example-data.csv example-output.xls

### Running Linked Data Pipelines (Grafts)

To output linked data (RDF) you must call a graft (a pipeline that
converts `tabular data -> linked data`).  Like with `defpipe` grafts
must be exposed to the plugin with `defgraft`.

They can be run in the same way as pipes e.g to output linked data as
turtle:

    $ lein grafter run test-project.pipeline/convert-persons-data-to-graph ./data/example-data.csv example-output.ttl

... or as n-triples:

    $ lein grafter run test-project.pipeline/convert-persons-data-to-graph ./data/example-data.csv example-output.nt

Supported formats for triples and their file extensions are:

- n-triples `.nt`
- turtle `.ttl`
- n3 `.n3`
- rdf xml `.rdf`

Supported formats for quads and their file extensions are:

- n-quads `.nq`
- trig `.trig`
- trix `.trix`

## License

Copyright © 2014 Swirrl IT Ltd

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
