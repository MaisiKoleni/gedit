package experiments;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Scanner;
import gedit.model.Document;
import gedit.model.DocumentAnalyzer;
import gedit.model.FileProzessor;

public class AnalyzeJavaGrammar {

	public static void main(String[] args) throws IOException {
		Document doc = new Document();
		DocumentAnalyzer docAnalyzer = new DocumentAnalyzer(null, new FileProzessor(), doc);
		String grammarText = Files.readString(Path.of(args[0]));

		try (var x = new Scanner(System.in)) {
			System.out.println("Press enter to start mini-benchmark");
			x.nextLine();
		}

		Instant i1 = Instant.now();
		for (int i = 0; i < 1000; i++) {
			Instant ix = Instant.now();
			docAnalyzer.analyze(doc, grammarText);
			printDT(ix);
		}
		printDT(i1);
		// 25.5s initial
		// 04.5s after null cache getElementById
	}

	static void printDT(Instant start) {
		var dt = Duration.between(start, Instant.now());
		System.out.format("%01d.%04d%n", dt.getSeconds(), dt.toNanosPart() / 100_000);
	}
}
