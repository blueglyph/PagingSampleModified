Visual issues with custom PagingSource with PagingDataAdapter
=============================================================

This application is a modification of architecture-components-samples-master\PagingSample in the following project:

  https://github.com/android/architecture-components-samples/tree/master/PagingSample

downloaded from revision cb6140f43d82e611498d755423c2d915a0c7ee7f (27 Oct 2021)

It has been simplified to showcase a bug when using a custom PagingSource with PagingDataAdapter.
- the Room part has been removed
- data is now in a Map<Int, Cheese>, with Cheese::id as key
- sorted data is shown on the RecyclerView, with the position index (which is also Cheese::id) conveniently shown in the name
- page size has been reduced to 30, memory retention to 90, to make it less power-consumming and more realistic


App Description
---------------

### Features

This sample contains a single screen with a list of text items. Items can be added to the list with the input at the top,
and swiping items in the list removes them.

Bug Description
---------------

When 2 pages are shown simultaneously on the screen, invalidating the PagingSource - when inserting or deleting an item -
shows glitches, as if many items had been modified instead of one.

When all shown items belong to a unique page, the behaviour is as expected: only the inserted/removed item has an
animation, there are no glitches on other items.

### How to reproduce

- launch the application
- scroll down to place item [31] as first visible item at the top (=> all visible items belong to the same page)
- insert an item named "bac" for example

=> the animations are shown as expected

- delete the "bac" item

=> the animations are shown as expected

- scroll up to show item [29] as first visible item at the top
- insert an item named "bac" for example

=> the animations is glitchy, many items look as if they were modified

- delete the "bac" item

=> the animations is glitchy, many items look as if they were modified

On top of that, logging the work in DiffUtil.ItemCallback shows an excessive amount of comparisons; more than 2000,
which is 10 times the expected work.

### The original app

If you do the same test on the original app, which takes the data from Room thus generating PagingSource automatically,
the bug is not visible. However, the code is too complicated to tell how the behaviour of PagingSource differs from the
custom version.


License
-------

(Kept from the original app)


Copyright 2017 The Android Open Source Project, Inc.

Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
