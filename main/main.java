

import java.sql.*;

public class Main {

    private static final String URL  = "jdbc:mysql://localhost:3306/webbshop?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String USER = "webbshop_user";      
    private static final String PASS = "mitt_starka_losen";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(URL, USER, PASS)) {
            System.out.println("Ansluten till databasen.\n");

            runAndPrint(conn, "1) Kunder som köpt svarta byxor 38 (SweetPants)", """
                SELECT DISTINCT k.Namn
                FROM Kund k
                JOIN Bestallning b     ON b.KundID = k.KundID
                JOIN Bestallningsrad r ON r.BestallningsID = b.BestallningsID
                JOIN Vara v            ON v.VaraID = r.VaraID
                JOIN Marke m           ON m.MarkeID = v.MarkeID
                WHERE m.Namn = 'SweetPants'
                  AND v.Farg = 'Svart'
                  AND v.Storlek = '38'
            """);

            runAndPrint(conn, "2) Antal produkter per kategori", """
                SELECT k.Namn AS Kategori, COUNT(DISTINCT vk.VaraID) AS AntalProdukter
                FROM Kategori k
                LEFT JOIN VaraKategori vk ON vk.KategoriID = k.KategoriID
                GROUP BY k.KategoriID, k.Namn
                ORDER BY k.Namn
            """);

            runAndPrint(conn, "3) Total köpsumma per kund", """
                SELECT k.Namn,
                       SUM(r.Antal * COALESCE(r.PrisVidKop, v.Pris)) AS TotalSumma
                FROM Kund k
                JOIN Bestallning b     ON b.KundID = k.KundID
                JOIN Bestallningsrad r ON r.BestallningsID = b.BestallningsID
                JOIN Vara v            ON v.VaraID = r.VaraID
                GROUP BY k.KundID, k.Namn
                ORDER BY TotalSumma DESC
            """);

            runAndPrint(conn, "4) Beställningsvärde per ort (> 1000 kr)", """
                SELECT k.Ort,
                       SUM(r.Antal * COALESCE(r.PrisVidKop, v.Pris)) AS TotaltVarde
                FROM Kund k
                JOIN Bestallning b     ON b.KundID = k.KundID
                JOIN Bestallningsrad r ON r.BestallningsID = b.BestallningsID
                JOIN Vara v            ON v.VaraID = r.VaraID
                GROUP BY k.Ort
                HAVING TotaltVarde > 1000
                ORDER BY TotaltVarde DESC
            """);

            runAndPrint(conn, "5) Topp-5 mest sålda produkter", """
                SELECT v.Namn AS Produkt,
                       SUM(r.Antal) AS AntalSalda
                FROM Bestallningsrad r
                JOIN Vara v ON v.VaraID = r.VaraID
                GROUP BY v.VaraID, v.Namn
                ORDER BY AntalSalda DESC
                LIMIT 5
            """);

            runAndPrint(conn, "6a) Försäljning per månad (alla)", """
                SELECT DATE_FORMAT(b.Datum, '%Y-%m') AS Manad,
                       SUM(r.Antal * COALESCE(r.PrisVidKop, v.Pris)) AS Total
                FROM Bestallning b
                JOIN Bestallningsrad r ON r.BestallningsID = b.BestallningsID
                JOIN Vara v            ON v.VaraID = r.VaraID
                GROUP BY DATE_FORMAT(b.Datum, '%Y-%m')
                ORDER BY Total DESC
            """);

            runAndPrint(conn, "6b) Månad med störst försäljning (topp-1)", """
                SELECT DATE_FORMAT(b.Datum, '%Y-%m') AS Manad,
                       SUM(r.Antal * COALESCE(r.PrisVidKop, v.Pris)) AS Total
                FROM Bestallning b
                JOIN Bestallningsrad r ON r.BestallningsID = b.BestallningsID
                JOIN Vara v            ON v.VaraID = r.VaraID
                GROUP BY DATE_FORMAT(b.Datum, '%Y-%m')
                ORDER BY Total DESC
                LIMIT 1
            """);

        } catch (SQLException e) {
            System.err.println("JDBC-fel: " + e.getMessage());
        }
    }

    private static void runAndPrint(Connection conn, String title, String sql) {
        System.out.println("\n=== " + title + " ===");
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();

            // Skriv rubriker
            for (int c = 1; c <= cols; c++) {
                if (c > 1) System.out.print("\t");
                System.out.print(md.getColumnLabel(c));
            }
            System.out.println();

            // Skriv rader
            int rowCount = 0;
            while (rs.next()) {
                rowCount++;
                for (int c = 1; c <= cols; c++) {
                    if (c > 1) System.out.print("\t");
                    String val = rs.getString(c);
                    System.out.print(val == null ? "NULL" : val);
                }
                System.out.println();
            }
            if (rowCount == 0) System.out.println("(inga rader)");

        } catch (SQLException e) {
            System.err.println("Fel vid körning: " + e.getMessage());
        }
    }
}

