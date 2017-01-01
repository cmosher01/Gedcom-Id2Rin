package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Created by user on 12/7/16.
 */
public class GedcomId2Rin {
    private final File file;
    private Charset charset;
    private GedcomTree gt;



    public static void main(final String... args) throws InvalidLevel, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java -jar gedcom-id2rin in.ged >out.ged");
            System.exit(1);
        } else {
            new GedcomId2Rin(args[0]).main();
        }
    }



    private GedcomId2Rin(final String filename) {
        this.file = new File(filename);
    }

    public void main() throws IOException, InvalidLevel {
        loadGedcom();
        updateGedcom();
        saveGedcom();
    }

    private void loadGedcom() throws IOException, InvalidLevel {
        this.charset = Gedcom.getCharset(this.file);
        this.gt = Gedcom.parseFile(file, this.charset);
    }

    private void saveGedcom() throws IOException {
        final BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(FileDescriptor.out), this.charset));

        Gedcom.writeFile(this.gt, out, 120);

        out.flush();
        out.close();
    }



    private void updateGedcom() {
        removeNodes(findAnyExistingRins(this.gt.getRoot(), new ArrayList<>(4096)));
        addNodes(createNewRinsFromIds(this.gt.getRoot(), new ArrayList<>(4096)));
    }

    private List<TreeNode<GedcomLine>> findAnyExistingRins(final TreeNode<GedcomLine> node, final List<TreeNode<GedcomLine>> rins) {
        node.forEach(c -> findAnyExistingRins(c, rins));

        final GedcomLine gedcomLine = node.getObject();
        if (gedcomLine != null && gedcomLine.getTag().equals(GedcomTag.RIN)) {
            rins.add(node);
        }

        return rins;
    }

    private static class ChildToBeAdded {
        TreeNode<GedcomLine> parent;
        TreeNode<GedcomLine> child;
        ChildToBeAdded(TreeNode<GedcomLine> parent, TreeNode<GedcomLine> child) {
            this.parent = parent;
            this.child = child;
        }
    }

    private List<ChildToBeAdded> createNewRinsFromIds(final TreeNode<GedcomLine> root, final List<ChildToBeAdded> rins) {
        root.forEach(top -> {
            final GedcomLine gedcomLine = top.getObject();
            if (gedcomLine != null && gedcomLine.hasID()) {
                final TreeNode<GedcomLine> rin = new TreeNode<>(new GedcomLine(gedcomLine.getLevel() + 1, "", GedcomTag.RIN.name(), gedcomLine.getID()));
                rins.add(new ChildToBeAdded(top, rin));
            }
        });
        return rins;
    }



    private static void removeNodes(final List<TreeNode<GedcomLine>> nodes) {
        nodes.forEach(TreeNode::removeFromParent);
    }

    private static void addNodes(final List<ChildToBeAdded> adds) {
        adds.forEach(add -> {
            add.parent.addChild(add.child);
        });
    }
}
