package vs.hardcoredistro.jsf;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.omnifaces.cdi.Param;
import org.omnifaces.util.Faces;
import vs.hardcoredistro.auth.LoggedInUser;
import vs.hardcoredistro.entities.OrderedAlbum;
import vs.hardcoredistro.services.PaymentService;
import vs.hardcoredistro.services.PurchaseService;
import vs.hardcoredistro.services.StockService;

/**
 *
 * @author vasouv
 */
@Named
@RequestScoped
public class CheckoutBean {

    @Inject
    private CartBean cartBean;

    @Inject
    private StockService stockService;

    @Inject
    private PurchaseService purchaseService;

    @Inject
    private LoggedInUser loggedInUser;

    @Inject
    private PaymentService paymentService;

    private List<OrderedAlbum> albumsToOrder;

    @Inject
    @ConfigProperty(name = "apiKey")
    private String apiKey;

    // Injects the stripeToken field returned from Stripe once the payment is processed
    @Inject
    @Param
    private String stripeToken;

    // Injects the user's email returned from Stripe
    @Inject
    @Param
    private String email;

    @PostConstruct
    public void init() {
        albumsToOrder = new ArrayList<>(cartBean.getOrderedAlbums());

        // If the bean is created and has the stripeToken from the callback, make the purchase
        if (stripeToken != null && !stripeToken.isEmpty()) {
            createCharge();
        }
    }

    public String buy() {
        boolean purchaseCompleted = purchaseService.create(albumsToOrder, loggedInUser.getLoggedInUser());
        if (purchaseCompleted) {
            return "checkout-complete.xhtml";
        }
        showMessage();
        return "checkout.xhtml";
    }

    private void createCharge() {
        try {
            Charge charge = paymentService.charge(stripeToken, getTotal(), "EUR");
            if (charge != null) {
                Faces.redirect("user/checkout-complete.xhtml");
            } else {
                Faces.redirect("user/checkout-failure.xhtml");
            }
        } catch (StripeException ex) {
            Faces.redirect("user/checkout-failure.xhtml");
            ex.printStackTrace();
        }
    }

    private void showMessage() {
        FacesContext context = FacesContext.getCurrentInstance();
        context.addMessage(null, new FacesMessage("Some albums were not in availability. Please check quantity"));
    }

    public double getTotalAmount() {
        double total = 0.0;
        for (OrderedAlbum orderedAlbum : albumsToOrder) {
            total += orderedAlbum.getQuantity() * orderedAlbum.getAlbum().getPrice();
        }
        return total;
    }

    /*
    * 1. Rounding up to zero decimals to create integer
    * 2. Multiply *100 so we don't charge cents instead of EUR
    */
    public BigDecimal getTotal() {
        BigDecimal forStripe = new BigDecimal(getTotalAmount());
        forStripe = forStripe.setScale(0, BigDecimal.ROUND_UP);
        System.out.println("BigDecimal price: " + forStripe.toString());
        return forStripe.multiply(new BigDecimal("100"));
    }

    // ACCESSOR METHODS
    public List<OrderedAlbum> getAlbumsToOrder() {
        return albumsToOrder;
    }

    public void setAlbumsToOrder(List<OrderedAlbum> albumsToOrder) {
        this.albumsToOrder = albumsToOrder;
    }

    public String getApiKey() {
        return apiKey;
    }

}
