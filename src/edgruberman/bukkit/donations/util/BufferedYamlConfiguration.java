package edgruberman.bukkit.donations.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import com.google.common.io.Files;

/**
 * queues save requests to prevent occurring more than a maximum rate
 *
 * @author EdGruberman (ed@rjump.com)
 * @version 3.0.0
 */
public class BufferedYamlConfiguration extends YamlConfiguration implements Runnable {

    protected static final String NEWLINE_PLATFORM = System.getProperty("line.separator");
    protected static final Pattern NEWLINE_ANY = Pattern.compile("\\r?\\n");
    protected static final int TICKS_PER_SECOND = 20;

    protected final Plugin owner;
    protected File file;
    protected long rate;
    protected long lastSaveAttempt = -1;
    protected int taskSave = -1;
    protected Object lock = new Object();

    /** @param rate minimum time between saves (milliseconds) */
    public BufferedYamlConfiguration(final Plugin owner, final File file, final long rate) {
        this.owner = owner;
        this.file = file;
        this.rate = rate;
    }

    public Plugin getOwner() {
        return this.owner;
    }

    public File getFile() {
        return this.file;
    }

    public long getRate() {
        return this.rate;
    }

    public void setRate(final int rate) {
        this.rate = rate;
        if (!this.isQueued()) return;
        Bukkit.getScheduler().cancelTask(this.taskSave);
        this.queueSave();
    }

    public long getLastSaveAttempt() {
        return this.lastSaveAttempt;
    }

    public void clear() {
        this.map.clear();
    }

    public BufferedYamlConfiguration load() throws IOException, InvalidConfigurationException {
        try {
            super.load(this.file);
        } catch (final FileNotFoundException e) {
            this.loadFromString("");
        } catch (final IOException e) {
            throw e;
        } catch (final InvalidConfigurationException e) {
            throw e;
        }
        return this;
    }

    @Override
    public void load(final File file) throws FileNotFoundException, IOException, InvalidConfigurationException {
        this.file = file;
        this.load();
    }

    @Override
    public void save(final File file) throws IOException {
        this.file = file;
        super.save(file);
    }

    /** force immediate save */
    public boolean save() {
        try {
            super.save(this.file);

        } catch (final IOException e) {
            this.owner.getLogger().log(Level.SEVERE, "Unable to save configuration file: {0}; {1}", new Object[] { this.file, e });
            return false;

        } finally {
            this.lastSaveAttempt = System.currentTimeMillis();
        }

        this.owner.getLogger().log(Level.FINEST, "Saved configuration file: {0}", this.file);
        return true;
    }

    public void queueSave() {
        final long elapsed = System.currentTimeMillis() - this.lastSaveAttempt;

        if (elapsed < this.rate) {
            final long delay = this.rate - elapsed;

            if (this.isQueued()) {
                this.owner.getLogger().log(Level.FINEST
                        , "Save request already queued to run in {0} seconds for file: {1} (Last attempted {2} seconds ago)"
                        , new Object[] { delay / 1000, this.getFile(), elapsed / 1000 });
                return;
            }

            // schedule task to flush cache to file system
            this.taskSave = Bukkit.getScheduler().scheduleSyncDelayedTask(this.owner, this, delay / 1000 * BufferedYamlConfiguration.TICKS_PER_SECOND);
            this.owner.getLogger().log(Level.FINEST
                    , "Queued save request to run in {0} seconds for configuration file: {1} (Last attempted {2} seconds ago)"
                    , new Object[] { delay / 1000, this.getFile(), elapsed / 1000 });
            return;
        }

        this.run();
    }

    @Override
    public void run() {
        final String data = this.saveToString();
        final Logger logger = new SynchronousPluginLogger(this.owner);
        final Runnable writer = new AsynchronousWriter(this.file, data, logger, this.lock);
        Bukkit.getScheduler().runTaskAsynchronously(this.owner, writer);
        this.taskSave = -1;
    }

    public boolean isQueued() {
        return Bukkit.getScheduler().isQueued(this.taskSave);
    }

    public void cancelSave() {
        Bukkit.getScheduler().cancelTask(this.taskSave);
    }



    protected class AsynchronousWriter implements Runnable {

        protected final File file;
        protected final String data;
        protected final Logger logger;
        protected final Object lock;

        public AsynchronousWriter(final File file, final String data, final Logger logger, final Object lock) {
            this.file = file;
            this.data = data;
            this.logger = logger;
            this.lock = lock;
        }

        @Override
        public synchronized void run() {
            try {
                synchronized (this.lock) {
                    if (!this.file.getParentFile().exists()) Files.createParentDirs(this.file);
                    final Writer writer = new BufferedWriter(new FileWriter(this.file));
                    try {
                        writer.write(BufferedYamlConfiguration.NEWLINE_ANY.matcher(this.data).replaceAll(BufferedYamlConfiguration.NEWLINE_PLATFORM));
                    } finally {
                        writer.close();
                    }
                }
            } catch (final IOException e) {
                this.logger.log(Level.SEVERE, "Unable to save configuration file: {0}; {1}", new Object[] { this.file, e });
            } finally {
                BufferedYamlConfiguration.this.lastSaveAttempt = System.currentTimeMillis();
            }
            this.logger.log(Level.FINEST, "Saved configuration file: {0}", this.file);
        }

    }

}
