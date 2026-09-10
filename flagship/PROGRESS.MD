# PROGRESS LOG

2026-09-09   - Created a command line runner spring boot application
             - Figured out how to run it as a batch script
             - Added a simple file read using buffered reader
             - GOALS FOR THE NEXT DAY: Parse a file(preferrably pdf or excel or smth, more challenging than md or txt files)

2026-09-10   - Built a hand-rolled PDF text extractor to understand what's actually  happening under the hood. 
                Hit two real bug on the first test PDF: stacked 
                ASCII85+Flate encoding needing reversal in the right order, and ReportLab 
                omitting the newline before "endstream" when /Length is known. Fixed both by 
                reading actual byte output instead of guessing. Now switching to PDFBox for 
                the actual Phase 1 build — keeping this as a reference for why PDF parsing is genuinely nontrivial.

2026-09-10   - Built a simple pdf text extractor with PDFBox
                No bugs whatsoever, PDF is formatted
                same procedure, convert to byte then extract text 
                phase 1 complete, creating a document parsing