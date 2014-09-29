package com.quantimodo.etl.readers;

import com.quantimodo.etl.DatabaseView;

import java.io.File;
import java.io.IOException;

public interface Reader {
    DatabaseView getDatabaseView(CharSequence filename) throws IOException;

    DatabaseView getDatabaseView(File file) throws IOException;
}
