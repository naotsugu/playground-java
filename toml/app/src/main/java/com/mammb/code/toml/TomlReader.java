package com.mammb.code.toml;

import java.io.Closeable;
import com.mammb.code.toml.TomlValue.*;

public interface TomlReader extends Closeable {

    TomlObject readObject();

}
