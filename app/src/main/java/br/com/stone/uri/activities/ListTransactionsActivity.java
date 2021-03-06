package br.com.stone.uri.activities;

import android.content.Intent;
import android.net.Uri;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import br.com.stone.uri.R;
import br.com.stone.uri.code.Response;
import br.com.stone.uri.database.Transaction;
import butterknife.Bind;
import butterknife.OnItemClick;
import com.activeandroid.query.Select;
import com.jgabrielfreitas.core.activity.BaseActivity;
import com.jgabrielfreitas.layoutid.annotations.InjectLayout;
import java.util.ArrayList;
import java.util.List;

import static android.content.Intent.ACTION_VIEW;
import static java.lang.String.format;
import static java.lang.String.valueOf;

@InjectLayout(layout = R.layout.activity_list_transcations) public class ListTransactionsActivity extends BaseActivity {

  private static final int CANCELLATION_RESULT = 10;
  @Bind(R.id.transactionsListView) ListView transactionsListView;
  List<Transaction> transactions = new Select().from(Transaction.class).execute();

  @Override protected void onResume() {
    super.onResume();

    ArrayList<String> transactionsAsString = new ArrayList<>();

    for (Transaction transaction : transactions)
      transactionsAsString.add(format("ATK: %s\nStatus: %s", transaction.getAcquirerTransactionKey(), (transaction.isWasApproved()) ? "Aprovada" : "Negada" ));

    ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, android.R.id.text1, transactionsAsString);
    transactionsListView.setAdapter(adapter);
  }

  @OnItemClick(R.id.transactionsListView) public void onTransactionCLicked(int position) {

    // get transaction from view
    Transaction transaction = new Select().from(Transaction.class).where("ID = ?", position + 1).executeSingle();

    // create a new URI to request a cancel
    Uri.Builder transactionUri = new Uri.Builder();
    transactionUri.scheme("stone");
    transactionUri.authority("cancel");
    transactionUri.appendQueryParameter("AcquirerTransactionKey", transaction.getAcquirerTransactionKey());

    Intent intent = new Intent(ACTION_VIEW);
    intent.setDataAndType(transactionUri.build(), "text/plain");
    startActivityForResult(intent, CANCELLATION_RESULT);
  }

  @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (data != null && data.getData() != null) {

      Response response = new Response(data.getData());
      for (Transaction transaction : transactions)
        if (transaction.getAcquirerTransactionKey().equals(response.getAcquirerTransactionKey())) {
          transaction.updateStatus(response.wasApproved());
          killThisActivity();
          break;
        }
    }
  }

}
