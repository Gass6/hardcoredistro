package vs.hardcoredistro.jsf.admin;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

import vs.hardcoredistro.entities.Purchase;
import vs.hardcoredistro.services.PurchaseService;

@Named
@RequestScoped
public class PurchasesManager {

	@Inject
	private PurchaseService purchaseService;

	private List<Purchase> purchases;

	@PostConstruct
	public void init() {
		purchases = new ArrayList<>(purchaseService.findAll());
	}

	public List<Purchase> getPurchases() {
		return purchases;
	}

}