package test;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DocumentReference;

@Document("person")
public class PersonPo {
    @Id
    private String id;
    private String name;
    @DBRef
    private PersonPo mother;
    @DocumentReference
    private PersonPo father;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PersonPo getMother() {
        return mother;
    }

    public void setMother(PersonPo mother) {
        this.mother = mother;
    }

    public PersonPo getFather() {
        return father;
    }

    public void setFather(PersonPo father) {
        this.father = father;
    }
}
